# ColorOS launcher ↔ gesture navigation coupling

Static analysis of the Oplus (ColorOS / OxygenOS) home launcher, done to find
where full-screen gesture navigation gets tied to the stock launcher, and to
separate what AOSP enforces from what the Oplus launcher APK adds on top.

## Sample and method

| Item | Value |
|---|---|
| Binary | `OPlusLauncher.apk` from [reiryuki/OPlus-Launcher-34-Magisk-Module](https://github.com/reiryuki/OPlus-Launcher-34-Magisk-Module) (sourced from APKMirror `com.android.launcher` by OnePlus Ltd.) |
| Version | `140000034` / `14.0.34` (ColorOS/OxygenOS 14 codebase), minSdk 29, targetSdk 33, 6 dex files (~31 MB) |
| Identity | The porter renamed the manifest package to `com.oplus.launcher`. The original name is still `com.android.launcher`: it is the package in `resources.arsc`, and it appears in leftovers such as `taskAffinity="com.android.launcher.locktask"`. `sharedUserId="oppo.uid.launcher"`. |
| Tools | jadx 1.5.1 (dex → Java), androguard (manifest/ARSC/overlay) |
| Framework reference | AOSP `frameworks/base` @ main (aosp-mirror): `RecentTasks`, `ActivityTaskManagerService`, `OverviewProxyService`, `InputManagerService`, `core/res/.../config.xml` |

**Limits.** Oplus's own `SystemUI.apk`, `oplus-services.jar` and
`framework-res` overlays were **not** available, so the Oplus-specific
SystemUI/framework side is inferred from the launcher's client-side ABI. Those
inferences are marked as such below. Decompiled names are Oplus's own, because
the classes are not obfuscated. Only small helper lambdas are minified.

---

## TL;DR

The coupling sits in three layers:

1. **AOSP framework (hard, in every ROM):** gestures are not handled by the
   home app. A single system app, named by `config_recentsComponentName`, hosts
   `TouchInteractionService`. On ColorOS that app is the stock launcher
   (`com.android.launcher/com.android.quickstep.RecentsActivity`). SystemUI
   binds to it only when the service resolves with `MATCH_SYSTEM_ONLY`, and it
   needs signature permissions. **A user-installed launcher can never be the
   gesture host.** This is a framework restriction, not an Oplus choice.
2. **AOSP Quickstep fallback (in every ROM, the "laggy" path):** when the
   default HOME is not the gesture host, `OverviewComponentObserver` switches to
   `FallbackActivityInterface` + `FallbackSwipeHandler`. Swipe-up animates
   into the launcher's own `RecentsActivity`, then starts the third-party home with
   `startActivity(homeIntent)`. It only gets a smooth hand-off if that launcher
   implements `GestureNavContract`.
3. **Oplus launcher APK (the ColorOS-specific coupling):**
   `com.oplus.quickstep.gesture.helper.SpecialSceneHelper`, invoked from
   `com.android.quickstep.OplusBaseTouchInteractionService`. When a different
   default home is in the foreground, it **replaces the gesture pipeline
   with `NonGestureInputConsumer`**. That consumer waits for an up-fling,
   then asks Oplus SystemUI over a **private `ISystemUiProxy` transaction (154)**
   to inject `KEYCODE_HOME`. This is the subsystem behind the stock-only
   behaviour, and it is pure APK logic.

---

## 1. Android framework restrictions (AOSP; ColorOS inherits them)

These hold for any OEM, Pixel included. No launcher-side change can get around
them without root or a system-partition overlay.

### 1.1 One "recents component" decides who owns gestures

`core/res/res/values/config.xml`:

```xml
<string name="config_recentsComponentName" translatable="false"
        >com.android.launcher3/com.android.quickstep.RecentsActivity</string>
```

The OEM overrides this through a static RRO on `android`. The port ships the same
override, which shows exactly what ColorOS sets
(`OPlusLauncherRecentsOverlay.apk`, `targetPackage="android"`, `isStatic="true"`):

```xml
<string name="config_recentsComponentName">com.oplus.launcher/com.android.quickstep.RecentsActivity</string>
<string name="config_secondaryHomePackage">com.oplus.launcher</string>
```

On stock ColorOS the value is `com.android.launcher/com.android.quickstep.RecentsActivity`.
It is a read-only resource, so the user cannot change it at runtime.

### 1.2 SystemUI binds gestures only to that package, and only if it is a system app

`packages/SystemUI/.../recents/OverviewProxyService.java`:

```java
mRecentsComponentName = ComponentName.unflattenFromString(context.getString(
        com.android.internal.R.string.config_recentsComponentName));
mQuickStepIntent = new Intent(ACTION_QUICKSTEP)          // "android.intent.action.QUICKSTEP_SERVICE"
        .setPackage(mRecentsComponentName.getPackageName());
...
mIsEnabled = mContext.getPackageManager().resolveServiceAsUser(mQuickStepIntent,
        MATCH_SYSTEM_ONLY, currentUser) != null;
...
mBound = mContext.bindServiceAsUser(mQuickStepIntent, mOverviewServiceConnection, ...);
```

The launcher's manifest receives this binding:

```xml
<service android:name="com.android.quickstep.TouchInteractionService"
         android:permission="android.permission.STATUS_BAR_SERVICE" android:exported="true">
  <intent-filter><action android:name="android.intent.action.QUICKSTEP_SERVICE"/></intent-filter>
</service>
```

### 1.3 Privileged hooks are gated by UID or signature

| Gate | Where | Effect |
|---|---|---|
| `RecentTasks.loadRecentsComponent()` → `mRecentsUid` | `services/.../wm/RecentTasks.java` | `isCallerRecents(uid)` gives the recents UID unrestricted `getRecentTasks` (`ATMS.isGetTasksAllowed`) and recents-animation privileges. |
| `enforceTaskPermission(...)` (`MANAGE_ACTIVITY_TASKS`) | `ATMS.preloadRecentsActivity` and friends | Signature/privileged only. |
| `MONITOR_INPUT` | `InputManagerService.monitorGestureInput` | Needed for the gesture input monitor that `TouchInteractionService` opens. Signature permission. |
| `STATUS_BAR_SERVICE` | the service's `android:permission` | Only SystemUI can bind to it. |

**Conclusion:** the framework ties gesture handling to *the configured recents
package*, not to *the default HOME*. Which home is active only matters to the
code inside that package (§2 and §3).

---

## 2. AOSP Quickstep behaviour shipped inside the launcher (not Oplus-specific)

### 2.1 Deciding "home == overview"

`com/android/quickstep/OverviewComponentObserver.java` (`classes2.dex`), `updateOverviewTargets()`:

```java
ComponentName defaultHome = PackageManagerWrapper.getInstance().getHomeActivities(new ArrayList());
mIsDefaultHome = Objects.equals(mMyHomeIntent.getComponent(), defaultHome);
...
if (mDeviceState.isHomeDisabled() || !(defaultHome == null || mIsDefaultHome)) {
    mActivityInterface   = FallbackActivityInterface.INSTANCE;   // third-party home
    mIsHomeAndOverviewSame = false;
    mOverviewIntent      = mFallbackIntent;                       // own RecentsActivity
    mCurrentHomeIntent.setComponent(defaultHome);
} else {
    mActivityInterface   = LauncherActivityInterface.INSTANCE;   // stock home
    mIsHomeAndOverviewSame = true;
    ...
}
```

It listens for `ACTION_PREFERRED_ACTIVITY_CHANGED`, so changing the default
launcher re-runs this at runtime.

There is one Oplus addition here. If the device supports a taskbar and the new
home is not stock (the brick-mode package `com.oneplus.brickmode` is exempt),
it calls `TaskbarSettingsConfig.backupTaskbarSettingEnable()` and then
`setTaskbarSettingEnable(false)`. **Picking a third-party launcher turns the taskbar off.**

### 2.2 Swipe handler selection

`TouchInteractionService.getSwipeUpHandlerFactory()`:

```java
return !mOverviewComponentObserver.isHomeAndOverviewSame()
        ? mFallbackSwipeHandlerFactory : mLauncherSwipeHandlerFactory;
```

`FallbackSwipeHandler` launches the third-party home after the gesture is committed:

```java
Intent intent = new Intent(mGestureState.getHomeIntent());
fallbackHomeAnimationFactory.addGestureContract(intent, target.taskInfo); // EXTRA_GESTURE_CONTRACT
mContext.startActivity(intent, ActivityOptions.makeCustomAnimation(mContext, 0, 0).toBundle());
```

Home becomes visible only after a real activity start. If the third-party
launcher does not answer the `GestureNavContract` callback, there is no
app-to-icon hand-off. This is the visible "return-to-home delay", and it is
**AOSP behaviour**: Pixel shows the same with launchers that lack contract support.

---

## 3. Oplus launcher APK behaviour (the ColorOS-specific coupling)

### 3.1 The responsible subsystem

**`com.oplus.quickstep.gesture.helper.SpecialSceneHelper`** (`classes3.dex`)
plus its call site in **`com.android.quickstep.OplusBaseTouchInteractionService`**.

Call chain for each touch-down in gesture mode:

```
OplusBaseTouchInteractionService.newConsumer()
  └─ newBaseConsumer()
       └─ if (deviceState.isFullyGesturalNavMode() && useSpecialInputConsumer(state))
              return SpecialSceneHelper.get().createInputConsumer(...)   // ← diverted
          else
              return createOtherActivityInputConsumer(...)               // normal Quickstep
```

`useSpecialInputConsumer()` calls `SpecialSceneHelper.canUseGestureInputConsumer(topTask, observer, ownPkg)`:

```java
return isDifferentDefaultHome(topTask, observer)   // ← third-party launcher check
    || isZoomWindowShown(topTask)                   // Oplus freeform "zoom window"
    || isOtherFallbackHome(topTask, pkg)            // ACTIVITY_TYPE_HOME owned by self (fallback)
    || isForceExludedTask(topTask);                 // OplusSpecialListHelper deny-list
```

```java
private boolean isDifferentDefaultHome(RunningTaskInfo top, OverviewComponentObserver obs) {
    if (obs.isHomeAndOverviewSame()) return false;            // stock home → normal path
    ComponentName defaultHome = PackageManagerWrapper.getInstance().getHomeActivities(...);
    return defaultHome != null && top != null && defaultHome.equals(top.topActivity);
}
```

In practice, whenever a third-party launcher is the default and is in the
foreground, Oplus **removes the swipe from Quickstep's gesture pipeline**.
Nothing tracks the finger, there is no recents animation, and swipe-and-hold does
not reach the live recents view.

### 3.2 What it is replaced with

`SpecialSceneHelper.createInputConsumer()` → `com.oplus.quickstep.gesture.inputconsumer.NonGestureInputConsumer`.

- It tracks Y velocity. Only a fling past `quickstep_fling_threshold_velocity`
  after touch slop fires `mSwipeAction.accept(true)`. A motion pause fires `accept(false)`.
- `getSwipeAction`:
  - `true` → `OplusInputInjectorUtils.notifyInjectKeyEvent(3)`, that is **inject `KEYCODE_HOME`**
  - `false` → toggle overview through `OplusOverviewCommandHelperImpl` / `onOverviewToggle()`
    (the launcher's own fallback `RecentsActivity`)
- The same consumer, with recents disabled, also serves Oplus "special scenes":
  `CHILD_MODE`, `SUPER_POWERSAVE_MODE`, `FOCUS_MODE`, `STUDY_MODE`,
  `WELL_BEING_ASSISTANT_MODE`. `newConsumer()` also routes everything here when
  `AppFeatureUtils.isCustomizeRecentTaskDisabled()` is set.

### 3.3 Private launcher↔SystemUI ABI

`OplusInputInjectorUtils` calls `SystemUiProxy.notifyKeyEvent()` /
`notifyInjectEvent()` / `notifyInjectAllEvent()`. These are **Oplus additions** to
AOSP's `com.android.systemui.shared.recents.ISystemUiProxy`, numbered outside
the AOSP range:

| Txn | Method |
|---|---|
| 151 | `onGetAppIcon` |
| 152 | `notifyInjectAllEvent` |
| 153 | `notifyInjectEvent` |
| 154 | `notifyKeyEvent` ← the HOME-key injection |
| 155 | `changeMistouchBarVisiablity` |
| 156 | `changeBottomGestureAreaRatio` |
| 157 | `notifyAppTransition` |
| 158 | `notifySwipeToRecentFinished` |

*Inference:* Oplus SystemUI implements these, and the stock launcher's gesture
behaviour depends on them. That fits the port's own known issue, "Full gesture
navigation doesn't work", on non-Oplus SystemUI. Oplus also reads its own
setting `Settings.Secure.hide_navigationbar_enable` (2 = swipe-up, 3 = side
gestures) next to AOSP `navigation_mode`.

### 3.4 Other APK-level couplings found

- `OverviewComponentObserver` turns the taskbar off when a non-stock home is chosen (§2.1).
- `RecentsActivityCommand` plays the Oplus recents enter animation only when `isHomeAndOverviewSame()`.
- `OplusAbsOverviewProxyImpl.onOverviewHidden()` acts on the created activity only in the fallback case.
- `TouchInteractionService.preloadOverview()` skips the recents preload in button mode when home ≠ overview.

---

## 4. Responsibility matrix

| Symptom with a third-party launcher on ColorOS | Layer | Component |
|---|---|---|
| Third-party launcher cannot run the gestures itself | **Framework** | `config_recentsComponentName`, `OverviewProxyService` (`MATCH_SYSTEM_ONLY`), `MONITOR_INPUT`, `STATUS_BAR_SERVICE` |
| Recents UI is always the stock launcher's | **Framework** + AOSP Quickstep | recents component + `FallbackActivityInterface` |
| Swipe home from an app → delayed or unanimated home | AOSP Quickstep | `FallbackSwipeHandler.startHomeIntent()`, `GestureNavContract` |
| Swipe on the third-party home screen does nothing until a fast fling, no live tracking | **Oplus APK** | `SpecialSceneHelper.isDifferentDefaultHome` → `NonGestureInputConsumer` |
| Home-from-home is an injected HOME key | **Oplus APK** + Oplus SystemUI | `OplusInputInjectorUtils.notifyKeyEvent(3)` → `ISystemUiProxy` txn 154 |
| Taskbar disappears after switching launcher | **Oplus APK** | `OverviewComponentObserver` → `TaskbarSettingsConfig.setTaskbarSettingEnable(false)` |

## 5. Implications

- **Not fixable from a third-party launcher.** The framework layer (§1) rules it out.
  The most a launcher can do is implement `GestureNavContract` to smooth the
  §2 hand-off.
- **Fixable inside the stock launcher (root / system overlay):** remove the
  `isDifferentDefaultHome` clause in `SpecialSceneHelper.canUseGestureInputConsumer`
  (smali patch). Swipes on a third-party home would then go through
  `OtherActivityInputConsumer` + `FallbackSwipeHandler`, as on AOSP.
- **Swapping the recents provider** (the port's overlay approach, or the XDA
  "change default ColorOS launcher" methods) needs a replacement that provides
  `QUICKSTEP_SERVICE` as a system app *and* speaks Oplus SystemUI's extended
  `ISystemUiProxy`. Otherwise the Oplus-only features (§3.3) break.
