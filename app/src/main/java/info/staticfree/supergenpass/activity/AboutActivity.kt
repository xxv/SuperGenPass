package info.staticfree.supergenpass.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import info.staticfree.supergenpass.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.about_title)

        setContentView(R.layout.activity_about)
    }
}