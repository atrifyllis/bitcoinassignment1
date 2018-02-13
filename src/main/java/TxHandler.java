import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TxHandler {

    private final UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // 1
        Optional<UTXO> unmatchedUtxo = tx.getInputs().stream()
                .map(input -> new UTXO(input.prevTxHash, input.outputIndex))
                .filter(utxo -> !this.utxoPool.contains(utxo))
                .findAny();

        if (unmatchedUtxo.isPresent()) return false;
        // 2
        // the previous output of an input should be found in the pool. if not the transaction is invalid
        boolean allSignaturesValid = tx.getInputs().stream()
                .allMatch(input -> {
                    Transaction.Output output = this.utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
                    return output != null ? Crypto.verifySignature(output.address, tx.getRawDataToSign(tx.getInputs().indexOf(input)), input.signature) : false;
                });
        if (!allSignaturesValid) return false;
        // 3
        long distinctUtxosClaimed = tx.getInputs().stream()
                .map(input -> new UTXO(input.prevTxHash, input.outputIndex))
                .distinct()
                .count();
        if (distinctUtxosClaimed != tx.getInputs().size()) return false;
        // 4
        long negativeOutputValues = tx.getOutputs().stream().map(output -> output.value).filter(value -> value < 0).count();
        if (negativeOutputValues > 0) return false;
        // 5
        double sumOfInputs = tx.getInputs().stream()
                .map(input -> this.utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex)).value)
                .mapToDouble(Double::doubleValue)
                .sum();
        double sumfOfOutputs = tx.getOutputs().stream().map(output -> output.value).mapToDouble(Double::doubleValue).sum();
        if (sumOfInputs < sumfOfOutputs) return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        List<Transaction> validTxs = new ArrayList<>();

        Arrays.stream(possibleTxs)
                .forEach(tx -> {
                    if (isValidTx(tx)) {
                        validTxs.add(tx);
                        tx.getInputs().stream()
                                .forEach(input -> this.utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex)));
                        tx.getOutputs().stream()
                                .forEach(output -> this.utxoPool.addUTXO(new UTXO(tx.getHash(), tx.getOutputs().indexOf(output)), output));
                    }
                });

        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
