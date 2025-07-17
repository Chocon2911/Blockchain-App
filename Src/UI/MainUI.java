package Src.UI;

import Src.Model.Miner;

public class MainUI {
    public MainUI() {

    }

    public void display(Miner miner) {
        while (true) {
            System.out.println("====================");
            System.out.println("1. Start miner");
            System.out.println("2. Stop miner");
            System.out.println("3. Balance");
            System.out.println("4. Exit");
            System.out.println("====================");

            int choice = new java.util.Scanner(System.in).nextInt();

            if (choice == 1) {
                miner.startMine();
            }
            else if (choice == 2) {
                miner.stopMine();
            }
            else if (choice == 3) {
                Long balance = miner.getBlockchain().getBalance(miner.getWallet().getPublicKey());
                System.out.println("Balance: " + balance / 100000000L + "." + balance % 100000000L + " BTC");
            }
            else if (choice == 4) {
                miner.stopMine();
                System.exit(0);
            }
        }
    }
}
