package Model;

public class TxIn {
    public String prevTxId; // Hash của giao dịch trước
    public int outputIndex; // Vị trí của output
    public byte[] scriptSig; // Chữ ký (giả lập)

    public TxIn(String txid, int outputIndex, String scriptSig) {
        this.prevTxId = prevTxId;
        this.outputIndex = outputIndex;
        this.scriptSig = Wallet.getSign();
    }
    @Override
    public String toString() {
        return "TxIn{Previoustxid=" + prevTxId.substring(0, 8) + "... , outputIndex=" + outputIndex + "}";
    }
}
