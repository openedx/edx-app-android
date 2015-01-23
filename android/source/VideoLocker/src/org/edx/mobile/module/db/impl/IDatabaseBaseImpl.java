package org.edx.mobile.module.db.impl;

import java.util.LinkedList;
import java.util.Queue;

import org.edx.mobile.logger.OEXLogger;

import android.content.Context;

class IDatabaseBaseImpl implements Runnable {

    private DbHelper helper;
    private Queue<IDbOperation<?>> opQueue = new LinkedList<IDbOperation<?>>();
    private boolean isQueueProcessing = false;
    protected static final OEXLogger logger = new OEXLogger(IDatabaseBaseImpl.class.getName());
    
    public IDatabaseBaseImpl(Context context) {
        helper = new DbHelper(context);
    }
    
    @Override
    public void run() {
        if (isQueueProcessing) {
            // queue is already being processed, so return
            // this will NOT allow multiple threads to process operation queue
            return;
        }
        
        do {
            // mark queue being processed
            isQueueProcessing = true;
            
            IDbOperation<?> op = getNextQueuedOperation();
            
            if (op == null) {
                break;
            }

            // perform the datbase operation
            execute(op); 
        } while(true);
        
        // mark queue not being processed
        isQueueProcessing = false;
        logger.debug("All database operations completed, queue is empty");
    }
    
    /**
     * Executes given database operation. This is a blocking call.
     * Returns result of the operation.
     * @param op
     * @return 
     */
    private synchronized <T extends Object> T execute(IDbOperation<?> op) {
        // perform this database operation
        synchronized (helper) {
            logger.debug("Performing a database operation ...");
            T result = (T) op.requestExecute(helper.getDatabase());
            logger.debug(op.getClass() + " operation completed");
            
            return result;
        }
    }

    /**
     * Enqueues given database operation to the operation queue and starts processing the queue,
     * if not already started.
     * Operation is executed in a queue in background thread if callback is provided for the operation and this method returns null. 
     * Otherwise this is a blocking call and returns result object.
     * @param operation
     */
    public synchronized <T extends Object> T enqueue(IDbOperation<?> operation) {
        // execute right away if this operation doesn't have a callback to send back the result
        if (operation.getCallback() == null) {
            return execute(operation);
        }
        
        // add non-blocking operations to the queue and process in sequence 
        synchronized (opQueue) {
            opQueue.add(operation);
            logger.debug("New operation enqueued : " + operation.getClass().getName());
        }
        
        // start processing the queue as we have a database operation to be processed
        new Thread(this).start();
        
        return null;
    }
    
    /**
     * Returns and removes the next operation from the operation queue.
     * @return
     */
    private IDbOperation<?> getNextQueuedOperation() {
        synchronized (opQueue) {
            if (opQueue.isEmpty()) {
                return null;
            }
            
            return opQueue.remove();
        }
    }

    /**
     * Closes this database object.
     */
    public void release() {
        helper.close();
    }

}
