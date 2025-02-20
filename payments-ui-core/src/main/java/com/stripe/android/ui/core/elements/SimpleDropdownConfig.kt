package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SimpleDropdownConfig(
    @StringRes override val label: Int,
    private val items: List<DropdownItemSpec>
) : DropdownConfig {
    override val debugLabel = "simple_dropdown"

    override val displayItems: List<String> =
        items.map { it.displayText }

    override fun getSelectedItemLabel(index: Int) = displayItems[index]

    override fun convertFromRaw(rawValue: String) =
        items
            .firstOrNull { it.apiValue == rawValue }
            ?.displayText
            ?: items[0].displayText

    override fun convertToRaw(displayName: String) =
        items
            .filter { it.displayText == displayName }
            .map { it.apiValue }
            .firstOrNull()
}
