package Model;

import java.util.ArrayList;
import java.util.List;

import Main.Util;

public class Miner
{
    //==========================================Variable==========================================
    private final Blockchain blockchain;
    private Wallet wallet;
    private List<Thread> runningThreads;

    public Blockchain getBlockchain() { return this.blockchain; }
    public Wallet getWallet() { return this.wallet; }
    public Long getHashRate() {
        Long start = System.currentTimeMillis();
        Long end = System.currentTimeMillis();
        Long times = 0L;
        while (end - 1000 < start) {
            end = System.currentTimeMillis();
            times++;
        }

        return (times) / (end - start);
    }

    //========================================Constructor=========================================
    public Miner(Wallet wallet, Blockchain blockchain) {
        this.runningThreads = new ArrayList<>();
        this.wallet = wallet;
        this.blockchain = blockchain;
    }

    //===========================================Method===========================================
    public void startMine() {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    this.blockchain.mine(this.wallet);
                }
            } catch (Exception e) {
                System.out.println("Exception Mining Thread: " + e.getMessage());
            }

            System.out.println("Mining Thread is stopped.");
        });

        this.runningThreads.add(thread);
        thread.start();
    }

    public void stopMine() {
        for (Thread thread : this.runningThreads) {
            thread.interrupt();
        }

        this.runningThreads.clear();
        synchronized (this.blockchain) {
            Util.getInstance().SaveBlockchainJson(this.blockchain);
        }
        synchronized (this.wallet) {
            Util.getInstance().SaveWalletJson(this.wallet);
        }
        System.out.println("Threads stopped");
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }
}