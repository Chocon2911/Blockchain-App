package Data.Service;

import Data.Constrcutor.ServiceData;

public class CheckBalanceServiceData extends ServiceData {
    private final long balance;

    public CheckBalanceServiceData(long balance) {
        this.action = "check_balance";
        this.balance = balance;
    }

    public long getBalance() { return balance; }
}
