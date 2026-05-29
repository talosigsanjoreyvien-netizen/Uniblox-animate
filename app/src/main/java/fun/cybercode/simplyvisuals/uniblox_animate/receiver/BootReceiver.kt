package `fun`.cybercode.simplyvisuals.uniblox_animate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `fun`.cybercode.simplyvisuals.uniblox_animate.service.RecoveryService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if recovery is needed. 
            // Since we're in a receiver, we might need to check DB asynchronously 
            // but we can just start the service and let it check, or just check briefly.
            // For now, let's just start the service to demostrate the "power off" requirement.
            // In a real app, you'd check a flag.
            
            val serviceIntent = Intent(context, RecoveryService::class.java)
            context.startService(serviceIntent)
        }
    }
}
