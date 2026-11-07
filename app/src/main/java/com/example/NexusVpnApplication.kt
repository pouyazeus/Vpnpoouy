package com.example

import android.app.Application
import com.example.data.local.VpnPreferencesDataStore
import com.example.data.repository.VpnRepository
import com.example.monetization.AdMobController

class NexusVpnApplication : Application() {

    lateinit var preferencesDataStore: VpnPreferencesDataStore
        private set

    lateinit var vpnRepository: VpnRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesDataStore = VpnPreferencesDataStore(this)
        vpnRepository = VpnRepository(preferencesDataStore)
        AdMobController.initialize(this)
    }
}
