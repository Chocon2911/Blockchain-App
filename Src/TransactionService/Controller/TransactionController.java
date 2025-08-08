package TransactionService.Controller;

import TransactionService.Model.Transaction;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import TransactionService.Service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService txService = new TransactionService();

    @PostMapping
    public String createTransaction(@RequestBody Transaction tx) {
        boolean isValid = txService.validateTransaction(tx);
        if (!isValid) return "Invalid signature";

        String txId = txService.addTransaction(tx);
        return "Transaction accepted. ID: " + txId;
    }
}
