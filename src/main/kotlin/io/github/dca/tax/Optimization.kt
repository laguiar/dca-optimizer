package io.github.dca.tax

import io.github.dca.ZERO

/**
 * From a given list of transactions, this function calculates the balance among all BUY and SELL transactions.
 *
 * The remaining list contains only BUY transactions with adjusted quantities and ready to be used for
 * tax allowance calculations.
 *
 * @return List of adjusted BUY transactions
 */
internal fun consolidateTransactionHistory(transactions: List<Transaction>): List<Transaction> =
    transactions
        .groupBy { it.ticker }
        .flatMap { (_, assetTransactions) -> processAssetTransactions(assetTransactions) }
        .sortedBy { it.date }

private fun processAssetTransactions(transactions: List<Transaction>): List<Transaction> =
    transactions
        .sortedBy { it.date }
        .fold(listOf<Transaction>() to ZERO) { (adjustedTransactions, sold), transaction ->
            when (transaction.direction) {
                Direction.BUY -> {
                    val remaining = transaction.shares - sold
                    if (remaining > ZERO) {
                        adjustedTransactions + transaction.copy(shares = remaining) to ZERO
                    } else {
                        adjustedTransactions to sold - transaction.shares
                    }
                }

                Direction.SELL -> {
                    // get the oldest transaction - it will always have at least one BUY transaction
                    val oldestTransaction = adjustedTransactions.first()
                    val remaining = oldestTransaction.shares - transaction.shares
                    if (remaining > 0) {
                        // replace old transaction with updated one
                        val updatedTransaction = oldestTransaction.copy(shares = remaining)
                        listOf(updatedTransaction) + adjustedTransactions.drop(1) to ZERO
                    } else {
                        adjustNegativeRenamingSellTransaction(transaction, adjustedTransactions) to sold
                    }
                }
            }
        }.first

/**
 * Process the next items in search for BUY transactions that might need to be adjusted or removed
 *
 * @param transaction actual SELL transaction that quantity surpass the oldest BUY transaction
 * @param adjustedTransactions mutable list that receives the adjusted transactions
 * @return the adjustedTransactions list
 */
private fun adjustNegativeRenamingSellTransaction(
    transaction: Transaction,
    adjustedTransactions: List<Transaction>
): List<Transaction> =
    adjustTransactions(transaction.shares, adjustedTransactions.asSequence())
        .toList()

private fun adjustTransactions(sold: Double, adjustedTransactions: Sequence<Transaction>): Sequence<Transaction> {
    if (adjustedTransactions.none()) return emptySequence()
    val firstTransaction = adjustedTransactions.first()
    val remaining = sold - firstTransaction.shares
    return if (areMoreSharesLeftThanSoldOnes(remaining, firstTransaction.shares, sold)) {
        if (remaining != ZERO) {
            // replace old transaction with updated one
            sequenceOf(
                firstTransaction.copy(shares = firstTransaction.shares - sold)
            ) + adjustedTransactions.dropFirst()
        } else {
            adjustedTransactions.dropFirst()
        }
    } else {
        adjustTransactions(remaining, adjustedTransactions.dropFirst())
    }
}

private fun areMoreSharesLeftThanSoldOnes(
    remaining: Double,
    transactionShares: Double,
    sold: Double
) = remaining < transactionShares && sold <= transactionShares

private fun Sequence<Transaction>.dropFirst() = this.drop(1)
