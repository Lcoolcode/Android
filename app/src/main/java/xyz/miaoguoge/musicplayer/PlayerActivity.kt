package xyz.miaoguoge.musicplayer

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.miaoguoge.musicplayer.databinding.ActivityPlayerBinding
import java.util.*

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding

    val updateSeekbar = 1

    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateSeekbar -> {
                    val duration = Config.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()
                    val currentPosition = Config.mediaPlayer.currentPosition
                    binding.seekBar.progress = 100 * currentPosition / duration
                    val currentTime = String.format("%d:%02d", currentPosition / 1000 / 60, currentPosition / 1000 % 60)
                    binding.tvCurrentTime.text = currentTime
                    if (Config.inAutoNext) {
                        updateInfo()
                    }
                }
            }
        }
    }
    private val timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            if (Config.mediaPlayer.isPlaying && !binding.seekBar.isPressed) {
                handler.sendEmptyMessage(updateSeekbar) // 发送消息
            }
        }
    }
    private val timer = Timer()

    private fun setStarBtn() {
        val curSong = Global.getSongByFilename(Config.musicList[Config.currentMusic])
        if (curSong in Global.Favor) {
            Log.d("PlayerActivity", "curSong in Global.Favor")
            binding.btnStarNo.visibility = Button.INVISIBLE
            binding.btnStarYes.visibility = Button.VISIBLE
        } else {
            Log.d("PlayerActivity", "curSong not in Global.Favor")
            binding.btnStarNo.visibility = Button.VISIBLE
            binding.btnStarYes.visibility = Button.INVISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Config.mediaPlayer.isPlaying) {
            binding.btnPlay.visibility = Button.INVISIBLE
            binding.btnPause.visibility = Button.VISIBLE
        }
        if (Config.isLoaded) {
            updateInfo()
            setStarBtn()
            timer.schedule(timerTask, 0, 1000)
        }

        binding.btnPlay.setOnClickListener {
            binding.btnPlay.visibility = Button.INVISIBLE
            binding.btnPause.visibility = Button.VISIBLE
            Config.StartPlay()
            updateInfo()
            setStarBtn()
            if (!Config.isLoaded) {
                timer.schedule(timerTask, 0, 1000)
            }
            Config.isLoaded = true
        }
        binding.btnPause.setOnClickListener {
            binding.btnPause.visibility = Button.INVISIBLE
            binding.btnPlay.visibility = Button.VISIBLE
            Config.mediaPlayer.pause()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d("PlayerActivity", "seek to $progress")
                if (fromUser) {
                    val duration = Config.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()
                    val msec = progress * duration / 100
                    Config.mediaPlayer.seekTo(msec)
                    val currentTime = String.format("%d:%02d", msec / 1000 / 60, msec / 1000 % 60)
                    binding.tvCurrentTime.text = currentTime
                }
                if (progress >= 98) {
                    Config.mediaPlayer.stop()
                    Config.mediaPlayer.release()
                    Config.setNextIndex()
                    val assetManager = assets
                    val fd = assetManager.openFd(Config.musicList[Config.currentMusic])
                    Config.mediaPlayer = MediaPlayer()
                    Config.mediaPlayer.reset()
                    Config.mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    Config.mediaPlayer.prepare()
                    Config.mediaPlayer.start()
                    Config.mmr.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    updateInfo()
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.btnNext.setOnClickListener {
            Config.mediaPlayer.stop()
            Config.mediaPlayer.release()
            Config.setNextIndex()
            val assetManager = assets
            val fd = assetManager.openFd(Config.musicList[Config.currentMusic])
            Config.mediaPlayer = MediaPlayer()
            Config.mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            Config.mediaPlayer.prepare()
            Config.StartPlay()
            Config.mmr.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            updateInfo()
            binding.btnPlay.visibility = Button.INVISIBLE
            binding.btnPause.visibility = Button.VISIBLE
        }
        binding.btnPrevious.setOnClickListener {
            Config.mediaPlayer.stop()
            Config.mediaPlayer.release()
            val assetManager = assets
            if (Config.currentMusic > 0) {
                Config.currentMusic--
            } else {
                Config.currentMusic = Config.musicList.size - 1
            }
            val fd = assetManager.openFd(Config.musicList[Config.currentMusic])
            Config.mediaPlayer = MediaPlayer()
            Config.mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            Config.mediaPlayer.prepare()
            Config.StartPlay()
            Config.mmr.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            updateInfo()
            binding.btnPlay.visibility = Button.INVISIBLE
            binding.btnPause.visibility = Button.VISIBLE
        }

        binding.btnAllCycle.setOnClickListener {
            binding.btnAllCycle.visibility = Button.INVISIBLE
            binding.btnShuffle.visibility = Button.VISIBLE
            binding.btnSingleCycle.visibility = Button.INVISIBLE
            Config.playMode = "shuffle"
        }
        binding.btnShuffle.setOnClickListener {
            binding.btnShuffle.visibility = Button.INVISIBLE
            binding.btnSingleCycle.visibility = Button.VISIBLE
            binding.btnAllCycle.visibility = Button.INVISIBLE
            Config.playMode = "single"
        }
        binding.btnSingleCycle.setOnClickListener {
            binding.btnSingleCycle.visibility = Button.INVISIBLE
            binding.btnAllCycle.visibility = Button.VISIBLE
            binding.btnShuffle.visibility = Button.INVISIBLE
            Config.playMode = "all"
        }

        binding.btnStarNo.setOnClickListener {
            binding.btnStarNo.visibility = Button.INVISIBLE
            binding.btnStarYes.visibility = Button.VISIBLE
            //添加到收藏
            val filename = Config.musicList[Config.currentMusic]
            Log.d("PlayerActivity", filename)
            val curSong = Global.getSongByFilename(filename)!!
            Global.Favor.add(curSong)
            Toast.makeText(this, "收藏成功", Toast.LENGTH_SHORT).show()
        }
        binding.btnStarYes.setOnClickListener {
            binding.btnStarYes.visibility = Button.INVISIBLE
            binding.btnStarNo.visibility = Button.VISIBLE
            //从收藏中移除
            val curSong = Global.getSongByFilename(Config.musicList[Config.currentMusic])
            Global.Favor.remove(curSong)
            Toast.makeText(this, "取消收藏", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateInfo() {
        binding.tvSongTitle.text = Config.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        binding.tvSongArtist.text = Config.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val duration = Config.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()
        val endTime = String.format("%d:%02d", duration / 1000 / 60, duration / 1000 % 60)
        binding.tvEndTime.text = endTime
        val cover = Config.mmr.embeddedPicture
        if (cover != null) {
            val bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.size)
            binding.ivAlbumCover.setImageBitmap(bitmap)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_to_playlist -> {
                Toast.makeText(this, "添加到歌单", Toast.LENGTH_SHORT).show()
            }
            R.id.goto_album -> {
                val albumFragment = AlbumFragment()
                val intent = Intent(this, LocalMusicActivity::class.java)
                startActivity(intent)
            }
            R.id.goto_artist -> {
                val intent = Intent(this, LocalMusicActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}
