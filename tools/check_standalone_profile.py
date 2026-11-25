#!/usr/bin/env python3
"""Protege el flujo de perfil independiente tras splash y al guardar."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/huntersystem/app"
main=(src/"MainActivity.kt").read_text(encoding="utf-8")
screens=(src/"Screens.kt").read_text(encoding="utf-8")
profile=(src/"Profile.kt").read_text(encoding="utf-8")
nav=(src/"MonarchNavigationV42.kt").read_text(encoding="utf-8")
store=(src/"NativeStore.kt").read_text(encoding="utf-8")
checks={
 "onboarding tras carga":'MonarchBootScreen(error)' in main and
     'profileStandalone -> ProfileScreen(ui)' in main and
     'profileEditorOpen by mutableStateOf(store.profile==null)' in screens,
 "edicion desde avatar":'ui.go("profile")' in screens and
     'fun openProfile()' in screens and 'if(t=="profile" || store.profile==null)' in screens,
 "sin barra inferior en perfil":'if(hasProfile && !profileStandalone' in main and
     'SceneColumn(ui,showHeader=false)' in profile,
 "el perfil no es una pestaña":'setOf("objectives","stats","profile")' not in nav and
     'MonarchScreenTransition(ui.tab' in main,
 "guardar vuelve al Panel":profile.count('ui.profileSaved()')==2 and
     'tab="home"' in screens and 'fun profileSaved()' in screens,
 "persistencia y plan":'s.saveProfile(p,appearance,ui.pendingAvatar)' in profile and
     'save();refreshWeek()' in store and 'saveProfile(p:JSONObject' in store,
 "recuperacion mantiene prioridad":'store.readError!=null -> MonarchRecoveryScreen(ui,store.readError!!)' in main,
 "orientacion conserva editor":'putBoolean("profileOpen",it.profileEditorOpen)' in main and
     'getBoolean("profileOpen")' in main,
}
failures=[name for name,ok in checks.items() if not ok]
for name,ok in checks.items():
 print(("OK" if ok else "ERROR")+": "+name)
if failures:
 raise SystemExit(1)
