package ch.isbsib.sparql.bed;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;

public class BEDFileFilterReader implements
		CloseableIteration<Statement, QueryEvaluationException> {
	private final BlockingQueue<Statement> statements;
	private final FilterReaderRunner runner;
	private final String wait = "wait";
	private final static ExecutorService exec = Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
				int a = 0;

				@Override
				public Thread newThread(Runnable arg0) {

					return new Thread(arg0, "BEDFileFilterReader" + (a++));
				}
			});

	public BEDFileFilterReader(File samFile, Resource subj, URI pred,
			Value obj, Resource[] contexts, ValueFactory valueFactory) {
		statements = new ArrayBlockingQueue<Statement>(1000);
		runner = new FilterReaderRunner(samFile, subj, pred, obj, statements,
				valueFactory, wait);
		exec.submit(runner);
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		while (!runner.done) {
			if (statements.peek() != null)
				return true;
			else
				try {
					synchronized (wait) {
						wait.wait();
					}
				} catch (InterruptedException e) {
					Thread.interrupted();
					return hasNext();
				}
		}
		return !statements.isEmpty();
	}

	@Override
	public Statement next() throws QueryEvaluationException {
		return statements.poll();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new QueryEvaluationException("Shizon does not support ");

	}

	@Override
	public void close() throws QueryEvaluationException {
		runner.done = true;

	}
}
