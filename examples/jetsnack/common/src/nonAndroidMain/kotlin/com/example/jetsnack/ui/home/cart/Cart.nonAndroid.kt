package com.example.jetsnack.ui.home.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jetsnack.MppR
import com.example.jetsnack.label_remove
import com.example.jetsnack.model.OrderLine
import com.example.jetsnack.model.SnackRepo
import com.example.jetsnack.model.SnackbarManager
import com.example.jetsnack.pluralsLocal
import com.example.jetsnack.stringResource
import com.example.jetsnack.ui.components.QuantitySelector
import com.example.jetsnack.ui.components.SnackImage
import com.example.jetsnack.ui.theme.JetsnackTheme
import com.example.jetsnack.ui.utils.formatPrice

@Composable
actual fun rememberQuantityString(res: Int, qty: Int, vararg args: Any): String {
    val plurals = pluralsLocal.current

    return remember(res, qty, plurals) {
        var str = plurals[res]?.forQuantity(qty) ?: ""
        args.forEachIndexed { index, any ->
            str = str.replace("%${index + 1}d", any.toString())
        }
        str
    }
}

@Composable
actual fun ActualCartItem(
    orderLine: OrderLine,
    removeSnack: (Long) -> Unit,
    increaseItemCount: (Long) -> Unit,
    decreaseItemCount: (Long) -> Unit,
    onSnackClick: (Long) -> Unit,
    modifier: Modifier
) {
    val snack = orderLine.snack

    Row(modifier = modifier
        .fillMaxWidth()
        .clickable { onSnackClick(snack.id) }
        .background(JetsnackTheme.colors.uiBackground)
        .padding(horizontal = 24.dp)
    ) {
        SnackImage(
            imageUrl = snack.imageUrl,
            contentDescription = null,
            modifier = Modifier.padding(top = 4.dp).size(100.dp)
        )
        Column(modifier = Modifier.padding(12.dp).weight(1f)) {
            Text(
                text = snack.name,
                style = MaterialTheme.typography.subtitle1,
                color = JetsnackTheme.colors.textSecondary,
            )
            Text(
                text = snack.tagline,
                style = MaterialTheme.typography.body1,
                color = JetsnackTheme.colors.textHelp,
            )
            Text(
                text = formatPrice(snack.price),
                style = MaterialTheme.typography.subtitle1,
                color = JetsnackTheme.colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            IconButton(
                onClick = { removeSnack(snack.id) },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = JetsnackTheme.colors.iconSecondary,
                    contentDescription = stringResource(MppR.string.label_remove)
                )
            }
            QuantitySelector(
                count = orderLine.count,
                decreaseItemCount = { decreaseItemCount(snack.id) },
                increaseItemCount = { increaseItemCount(snack.id) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }

}

@Composable
actual fun getCartContentInsets(): WindowInsets {
    return WindowInsets(top = 56.dp)
}

@Composable
actual fun provideCartViewModel(): CartViewModel {
    return remember { CartViewModel(SnackbarManager, SnackRepo) }
}