//
//  TurnoClase
//  Copyright 2015 Ion Jaureguialzo Sarasola.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.jaureguialzo.turnoclase

import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (!BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }

        // Pre-calentar tokens de App Check y Auth en segundo plano al arrancar la app.
        // Así el refresco está listo cuando el usuario pulse "conectar", evitando que
        // la renovación de tokens agote el timeout de la primera conexión.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                firebaseAppCheck.getToken(false).await()
            }
            runCatching {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()
            }
        }
    }
}
