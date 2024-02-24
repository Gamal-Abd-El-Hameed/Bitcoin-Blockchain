/*
“I acknowledge that I am aware of the academic integrity guidelines of this
course, and that I worked on this assignment independently without any
unauthorized help with coding or testing.” - <جمال عبد الحميد ناصف نويصر>
 */
// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {
    private class BlockNode {
        private Block block;
        private int height;
        private UTXOPool utxoPool;

        public BlockNode(Block block, UTXOPool utxoPool) {
            this.block = block;
            this.utxoPool = utxoPool;
        }
    }

    private HashMap<ByteArrayWrapper, BlockNode> H;
    private BlockNode maxHeightBlockNode;
    private BlockNode minHeightBlockNode;
    private TransactionPool globalTransactionPool;

    public static final int CUT_OFF_AGE = 10;

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        H = new HashMap<>();
        globalTransactionPool = new TransactionPool();
        UTXOPool utxoPool = new UTXOPool();
        Transaction coinbase = genesisBlock.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++)
            utxoPool.addUTXO(new UTXO(coinbase.getHash(), i), coinbase.getOutput(i));
        BlockNode genesisBlockNode = new BlockNode(genesisBlock, utxoPool);
        genesisBlockNode.height = 1;
        H.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisBlockNode);
        maxHeightBlockNode = genesisBlockNode;
        minHeightBlockNode = genesisBlockNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightBlockNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightBlockNode.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return globalTransactionPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null)
            return false;
        BlockNode parentBlockNode = H.get(new ByteArrayWrapper(prevBlockHash));
        if (parentBlockNode == null)
            return false;
        if (1 + parentBlockNode.height <= maxHeightBlockNode.height - CUT_OFF_AGE)
            return false;
        TxHandler txHandler = new TxHandler(parentBlockNode.utxoPool);
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = txHandler.handleTxs(blockTxs);
        if (validTxs.length != blockTxs.length)
            return false;
        UTXOPool utxoPool = txHandler.getUTXOPool();
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++)
            utxoPool.addUTXO(new UTXO(coinbase.getHash(), i), coinbase.getOutput(i));
        BlockNode blockNode = new BlockNode(block, utxoPool);
        blockNode.height = 1 + parentBlockNode.height;
        H.put(new ByteArrayWrapper(block.getHash()), blockNode);
        if (blockNode.height > maxHeightBlockNode.height)
            maxHeightBlockNode = blockNode;
        if (minHeightBlockNode.height < maxHeightBlockNode.height - CUT_OFF_AGE) {
            H.remove(new ByteArrayWrapper(minHeightBlockNode.block.getHash()));
            minHeightBlockNode = maxHeightBlockNode;
            while (minHeightBlockNode.height >= maxHeightBlockNode.height - CUT_OFF_AGE) {
                BlockNode parent = H.get(new ByteArrayWrapper(minHeightBlockNode.block.getPrevBlockHash()));
                if (parent == null)
                    break;
                minHeightBlockNode = parent;
            }
        }
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        globalTransactionPool.addTransaction(tx);
    }
}

/**
 * Additional Exercise:
 *
 * The problematic scenario:
 * The code will compute the hash of the entire COINBASE transaction, including the first field,
 * which is always zero and the second field, which is always one. This may lead to a hash collision.
 *
 * The actual implementation of Bitcoin:
 * prevents this issue by:
 * 1. Arbitrary data is used for extra nonce and mining tags.
 * 2. requiring that enough blocks have been mined on top of the block with that COINBASE transaction
 * such that it is extremely unlikely to be reorganized out of the main chain. This ensures the
 * COINBASE transaction is valid and the hash of the transaction is unique.
 *
 * References used in Additional Exercise:
 * https://developer.bitcoin.org/reference/transactions.html#coinbase-input-the-input-of-the-first-transaction-in-a-block
 * https://learn.saylor.org/mod/book/view.php?id=36375&chapterid=19427
 */