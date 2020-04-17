package com.example.TFMCA_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication
public class TfmcaServerApplication {
	public static final Integer ID_PRUNER_LOOP_MINS = 60;

	public static void main(String[] args) {
		SpringApplication.run(TfmcaServerApplication.class, args);
		DatabaseHandler.initialize();

		IdPruner id_pruner = new IdPruner();
		Timer id_pruner_timer = new Timer();

		id_pruner_timer.schedule(id_pruner, 100, ID_PRUNER_LOOP_MINS * 60000);
		WebSocketHandler.initHeartbeat();
	}
}

class IdPruner extends TimerTask {
	public void run() {
		SessionIdHandler.pruneIdMap();
	}
}