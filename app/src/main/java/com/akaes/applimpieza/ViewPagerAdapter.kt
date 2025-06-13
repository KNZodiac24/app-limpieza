package com.akaes.applimpieza

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter (fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PendientesFragment()
            1 -> EnProcesoFragment()
            2 -> CompletadosFragment()
            else -> throw IllegalStateException("Invalid tab position")
        }
    }
}