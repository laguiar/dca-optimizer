package io.github.dca.math.linear

/**
 * Pivot selection rules for the Simplex algorithm.
 */
enum class PivotSelectionRule {
    /**
     * Dantzig's rule: select the column with the most negative coefficient.
     * Generally faster but may cycle in degenerate cases.
     */
    DANTZIG,

    /**
     * Bland's rule: select the first column with negative coefficient.
     * Prevents cycling but may be slower.
     */
    BLAND
}