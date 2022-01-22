package com.complete.weathercheck.activities





import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment

import com.complete.weathercheck.R


import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstFragment=FirstFragment()
        val secondFragment=SecondFragment()


        setCurrentFragment(firstFragment)

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.home->setCurrentFragment(firstFragment)
                R.id.settings->setCurrentFragment(secondFragment)


            }
            true
        }

    }

    private fun setCurrentFragment(fragment: Fragment)=
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
        }
}