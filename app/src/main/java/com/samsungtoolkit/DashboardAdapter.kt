package com.samsungtoolkit

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

enum class BlockPos { SINGLE, TOP, MIDDLE, BOTTOM }

sealed class DashItem {
    abstract val stableId: Long
    data class Header(val section: FeatureSection) : DashItem() {
        override val stableId get() = ("h_" + section.id).hashCode().toLong()
    }
    data class Row(val feature: Feature, val pos: BlockPos, val showDivider: Boolean) : DashItem() {
        override val stableId get() = ("r_" + feature.id).hashCode().toLong()
    }
}

class DashboardAdapter(
    private val onFeatureClick: (Feature) -> Unit
) : ListAdapter<DashItem, RecyclerView.ViewHolder>(DIFF) {

    init { setHasStableIds(true) }

    override fun getItemId(pos: Int) = getItem(pos).stableId
    override fun getItemViewType(pos: Int) = if (getItem(pos) is DashItem.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (vt == 0)
            HeaderVH(inf.inflate(R.layout.item_section_header, parent, false))
        else
            RowVH(inf.inflate(R.layout.item_feature_row, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val item = getItem(pos)) {
            is DashItem.Header -> (holder as HeaderVH).bind(item.section)
            is DashItem.Row -> (holder as RowVH).bind(item, onFeatureClick)
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DashItem>() {
            override fun areItemsTheSame(a: DashItem, b: DashItem) = a.stableId == b.stableId
            override fun areContentsTheSame(a: DashItem, b: DashItem) = a == b
        }

        fun buildItems(sections: List<FeatureSection>): List<DashItem> {
            val result = mutableListOf<DashItem>()
            for (s in sections) {
                result.add(DashItem.Header(s))
                s.features.forEachIndexed { i, f ->
                    val pos = when {
                        s.features.size == 1 -> BlockPos.SINGLE
                        i == 0 -> BlockPos.TOP
                        i == s.features.lastIndex -> BlockPos.BOTTOM
                        else -> BlockPos.MIDDLE
                    }
                    result.add(DashItem.Row(f, pos, showDivider = i < s.features.lastIndex))
                }
            }
            return result
        }
    }
}

class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
    private val tv: TextView = v.findViewById(R.id.tvSectionTitle)
    fun bind(s: FeatureSection) { tv.text = s.title }
}

class RowVH(v: View) : RecyclerView.ViewHolder(v) {
    private val tvTitle: TextView = v.findViewById(R.id.tvTitle)
    private val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
    private val tvBadge: TextView = v.findViewById(R.id.tvBadge)
    private val divider: View = v.findViewById(R.id.divider)

    fun bind(item: DashItem.Row, onClick: (Feature) -> Unit) {
        tvTitle.text = item.feature.title
        tvSubtitle.text = item.feature.subtitle
        tvSubtitle.isVisible = item.feature.subtitle != null
        tvBadge.isVisible = item.feature.permission == PermissionLevel.SHIZUKU
        divider.isVisible = item.showDivider
        itemView.background = blockBg(itemView.context, item.pos)
        itemView.setOnClickListener { onClick(item.feature) }
    }

    private fun blockBg(ctx: Context, pos: BlockPos): Drawable {
        val r = 16f * ctx.resources.displayMetrics.density
        val shape = ShapeAppearanceModel.builder().apply {
            when (pos) {
                BlockPos.SINGLE -> setAllCornerSizes(r)
                BlockPos.TOP -> { setTopLeftCornerSize(r); setTopRightCornerSize(r) }
                BlockPos.BOTTOM -> { setBottomLeftCornerSize(r); setBottomRightCornerSize(r) }
                BlockPos.MIDDLE -> {}
            }
        }.build()
        val tv = TypedValue()
        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true)
        val bg = MaterialShapeDrawable(shape).apply { fillColor = ColorStateList.valueOf(tv.data) }
        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorControlHighlight, tv, true)
        return RippleDrawable(ColorStateList.valueOf(tv.data), bg, MaterialShapeDrawable(shape))
    }
}
