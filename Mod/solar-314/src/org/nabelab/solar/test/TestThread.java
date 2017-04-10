package org.nabelab.solar.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Conseq;
import org.nabelab.solar.ConseqSet;
import org.nabelab.solar.Env;
import org.nabelab.solar.Options;
import org.nabelab.solar.SOLAR;
import org.nabelab.solar.SOLAREvent;
import org.nabelab.solar.SOLARListener;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;

public class TestThread {

	private static class SOLARThread extends Thread {

		public SOLARThread(Env env, Options opt) throws FileNotFoundException, IOException, ParseException {
			this.env = env;
			this.opt = opt;
			CFP cfp = new CFP(env, opt);
			cfp.parse(new File(opt.getProblemFile()), opt.getBaseDir());
			solar = new SOLAR(env, cfp);          // Create a SOLAR system.
		}

		public void run() {
			try {
				solar.exec();
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
			System.out.println("SOLAR's symbol table");
			System.out.println(env.getSymTable());
		}

		public SOLAR getSOLAR() {
			return solar;
		}

		private Env env = null;
		private Options opt = null;
		private SOLAR solar = null;
	}

	private static class ListenerThread extends Thread implements SOLARListener {

		public ListenerThread() {
			this(new Env());
		}

		public ListenerThread(Env env) {
			this.env = env;
		}

		public void run() {
			try {
	      SOLAREvent event = null;
				String header = String.format("listener(%d)\t", Thread.currentThread().getId());
				while ((event = events.take()).getType() != SOLAREvent.FINISHED) {
					Clause found = event.getFoundClause();
					// clone the clause with the new environment which has a independent variable table.
					found = new Clause(env, found);
					System.out.println(header + "FOUND: " + found);
					conseqs.add(found);

					for (Clause removed : event.getRemovedClauses()) {
					// clone the clause with the new environment which has a independent variable table.
						removed = new Clause(env, removed);
						System.out.println(header + "REMOVED: " + removed);
						//conseqs.remove(removed);
					}
				}

				System.out.println(header + conseqs.size() + " FOUND CONSEQUENCES");

				// Computes subsumption minimal consequences
				env.initFVecMap(conseqs, null);
				ConseqSet cs = new ConseqSet(env);
				for (Clause c : conseqs)
					cs.add(c);
				System.out.println(header + cs.size() + " SUBSUMPTION MIN CONSEQUENCES");

				System.out.println("Listener's symbol table");
				System.out.println(env.getSymTable());

			} catch (InterruptedException e) {
				;
			}
		}

		public void conseqFound(SOLAREvent event) {
			String header = String.format("solar(%d)\t", Thread.currentThread().getId());
			System.out.println(header + "FOUND: " + event.getFoundClause().toSimpString());
			for (Clause removed : event.getRemovedClauses())
				System.out.println(header + "REMOVED: " + removed.toSimpString());
			events.add(event);
		}

		public void solarFinished(SOLAREvent event) {
			String header = String.format("solar(%d)\t", Thread.currentThread().getId());
			System.out.println(header + "SOLAR finished");
			events.add(event);
		}

		private Env env = null;
		private BlockingQueue<SOLAREvent> events = new LinkedBlockingQueue<SOLAREvent>();
		private List<Clause> conseqs = new LinkedList<Clause>();
	}

	private static class SenderThread extends Thread implements SOLARListener, ClauseTypes {

		public SenderThread(String carcFile) throws FileNotFoundException, ParseException {
			this(new Env(), carcFile);
		}

		public SenderThread(Env env, String carcFile) throws FileNotFoundException, ParseException {
			this.env = env;
			if (carcFile == null)
				return;
			Parser parser = new Parser(env, new Options(env));
			List<Conseq> conseqs = parser.conseqs(new BufferedReader(new FileReader(carcFile)));
			for (Conseq conseq : conseqs) {
				Clause carc = conseq.instantiate();         // make a copy.
				carc.setType(AXIOM);                      // change the type from CONSEQ to AXIOM.
				carcs.add(carc);
			}
		}

		public void run() {
			try {
				String header = String.format("sender(%d)\t", Thread.currentThread().getId());
				Random r = new Random();
				while (!finished && !carcs.isEmpty()) {
					long time = r.nextInt(1000);
					Thread.sleep(time);
					int num = r.nextInt(carcs.size());
					List<Clause> cs = new LinkedList<Clause>();
					for (int i=0; i < num; i++) {
						Clause c = carcs.pollFirst();
						cs.add(c);
						System.out.println(header + "SEND " + c);
					}
					// Sends characteristic clauses to SOLARs
					for (SOLAR solar : solars)
						solar.addCarcs(cs);
			}
			} catch (InterruptedException e) {
				;
			}
		}

		public void addReceiver(SOLAR solar) {
			solars.add(solar);
		}

		public void conseqFound(SOLAREvent event)   { }
		public void solarFinished(SOLAREvent event) { finished = true; System.out.println("finised = " + finished);}

		private Env env = null;
		private LinkedList<Clause> carcs = new LinkedList<Clause>();
		private List<SOLAR> solars = new ArrayList<SOLAR>();
		private boolean finished = false;
	}

	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ParseException, IOException {

		Env env = new Env();
    Options opt = new Options(env);             // Create the default options.
    opt.parse(args);                            // Analyze the command line arguments.

    //ListenerThread listenerThread = new ListenerThread();                                                           // uses different symbol table.
    ListenerThread listenerThread = new ListenerThread(new Env(env.getSymTable(), env.getDebug()));               // for sharing same symbol table.
    //SenderThread senderThread = new SenderThread(opt.getCarcFile());                                                // uses different symbol tale.
    SenderThread senderThread = new SenderThread(new Env(env.getSymTable(), env.getDebug()), opt.getCarcFile());  // for sharing same symbol table.

    SOLARThread solarThread = new SOLARThread(env, opt);

    // Add a listener object to receive SOLAR events.
    env.addSOLARListener(listenerThread);
    env.addSOLARListener(senderThread);

    // Add a receiver to the sender thread.
    senderThread.addReceiver(solarThread.getSOLAR());

    listenerThread.start();
    solarThread.start();
    senderThread.start();

	}

}
