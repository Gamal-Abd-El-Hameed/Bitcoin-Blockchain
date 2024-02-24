/*
“I acknowledge that I am aware of the academic integrity guidelines of this
course, and that I worked on this assignment independently without any
unauthorized help with coding or testing.” - <جمال عبد الحميد ناصف نويصر>
 */
import java.util.HashSet;

public class TxHandler {

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double sumOfInputValues = 0, sumOfOutputValues = 0;
        HashSet<UTXO> visitedUTXO = new HashSet<>();
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!utxoPool.contains(utxo))
                return false;
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature))
                return false;
            if (visitedUTXO.contains(utxo))
                return false;
            visitedUTXO.add(utxo);
            sumOfInputValues += output.value;
        }
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0)
                return false;
            sumOfOutputValues += output.value;
        }
        return sumOfInputValues >= sumOfOutputValues;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // TODO:
        // BONUS part isA
        HashSet<Transaction> acceptedTxs = new HashSet<>();
        boolean isNewTxFound = true;
        while (isNewTxFound) {
            isNewTxFound = false;
            for (Transaction tx : possibleTxs) {
                if (acceptedTxs.contains(tx) || !isValidTx(tx))
                    continue;
                isNewTxFound = true;
                acceptedTxs.add(tx);
                for (Transaction.Input input : tx.getInputs())
                    utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                for (int i = 0; i < tx.numOutputs(); i++)
                    utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
            }
        }
        return acceptedTxs.toArray(new Transaction[0]);
    }

}
