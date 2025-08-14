//package UI;
//
//import Model.Miner;
//import Service.MinerService;
//
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class MainUI {
//    public MainUI() {
//
//    }
//
//    public void display() {
//        while (true) {
//            System.out.println("====================");
//            System.out.println("1. Start miner");
//            System.out.println("2. Balance");
//            System.out.println("3. Exit");
//            System.out.println("====================");
//
//            int choice = new java.util.Scanner(System.in).nextInt();
//
//            if (choice == 1) {
//                this.displayStartMine();
//            }
//            else if (choice == 2) {
//                this.displayBalance();
//            }
//            else if (choice == 3) {
//                System.exit(0);
//            }
//        }
//    }
//
//    private void displayStartMine() {
//        int processorsLimit = 0;
//        while (true) {
//            int availableProcessorsAmount = MinerService.getAllProcessors() - 1;
//            System.out.println("Number of available threads: " + availableProcessorsAmount);
//            System.out.print("Enter number of threads: ");
//            processorsLimit = new java.util.Scanner(System.in).nextInt();
//            if (processorsLimit > availableProcessorsAmount) {
//                System.out.println("Too many threads, try again.");
//                continue;
//            }
//            break;
//        }
//
//        String publicAddress;
//        while (true) {
//            System.out.println("Enter public address: ");
//            publicAddress = new java.util.Scanner(System.in).nextLine();
//            if (publicAddress.length() != 42) {
//                System.out.println("Invalid public address, try again.");
//                continue;
//            }
//            break;
//        }
//
//        System.out.println("Mining...");
//        System.out.print("Enter 2 to stop: ");
//        AtomicBoolean canRun = new AtomicBoolean(true);
//        MinerService.startMine(processorsLimit, publicAddress, canRun);
//        int input = new java.util.Scanner(System.in).nextInt();
//        canRun.set(false);
//    }
//
//    private void displayBalance() {
//
//    }
//}
