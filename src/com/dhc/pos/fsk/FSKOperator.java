package com.dhc.pos.fsk;

import com.dhc.pos.agent.client.ApplicationEnvironment;

import android.content.Intent;
import android.os.Handler;

public class FSKOperator {
	
	public static Handler fskHandler = null;
	
	public static void execute(String fskCommand, Handler handler){
		fskHandler = handler;
		
		Intent intent  = new Intent(ApplicationEnvironment.getInstance().getApplication(), FSKService.class);
		intent.putExtra("FSKCOMMAND", fskCommand);
		ApplicationEnvironment.getInstance().getApplication().startService(intent);
	}

}
