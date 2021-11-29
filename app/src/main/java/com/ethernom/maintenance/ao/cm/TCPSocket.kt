package com.ethernom.maintenance.ao.cm

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethernom.maintenance.MainApplication
import com.ethernom.maintenance.ao.AoEvent
import com.ethernom.maintenance.ao.AoId
import com.ethernom.maintenance.ao.BROADCAST_INTERRUPT
import com.ethernom.maintenance.ao.EventBuffer
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

class TCPSocket(ctx: Context) {
    private val tag: String = javaClass.simpleName

    var SERVER_IP: String = "192.168.1.218"
    var SERVER_PORT = 8080

    private var context = ctx
    private val application: MainApplication = (context.applicationContext as MainApplication)
    private val intent = Intent(BROADCAST_INTERRUPT)
    private lateinit var tcpSocket:Socket
    private lateinit var output: DataOutputStream
    private lateinit var input: DataInputStream
    private var connected:Boolean = false
    var value: ByteArray = ByteArray(0)

    fun open() {
        Thread  {
            try {
                tcpSocket = Socket() // Initial Socket
            } catch (e: IOException) {
                Log.d(tag, "Open Error: ${e.message}")
            }
        }.start()
    }

    fun connect(ip:String, port:Int) {
        Thread {
            try {
                val serverAddress: SocketAddress = InetSocketAddress(ip, port) // Server Socket IP  & Port
                tcpSocket.connect(serverAddress) // Connect to Socket server
                output = DataOutputStream(tcpSocket.getOutputStream()) // Initial Output stream for send data to socket server
                input  = DataInputStream(tcpSocket.getInputStream()) // Initial Input stream for receive data from socket server
                connected = true

                val eventBuffer = EventBuffer(eventId = AoEvent.HTTP_DATA_REC)
                application.commonAO!!.sendEvent(AoId.AO_CM2_ID, eventBuffer)
                // send broadcast interrupt
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

                receive()
            } catch (e: IOException) {
                Log.d(tag, "Connect Error: ${e.message}")
                connected = false
            }
        }.start()
    }
    fun send (data : ByteArray) {
        Thread {
            if(connected) {
                try {
                    output.write(data) // writ data to socket
                    output.flush()
                } catch (e: IOException) {
                    Log.d(tag, "Send Error: ${e.message}")
                    close()
                }
            }
        }.start()
    }

    private fun receive() {
        while(connected){
            try {
                val data = ByteArray(2048)
                val size  = input.read(data) // Read data from socket
                Log.d(tag, "Receive Size: $size")

                if(size == -1) break
                else if(size != 0) {
                    val eventBuffer = EventBuffer(eventId = AoEvent.HTTP_DATA_REC, buffer = data.copyOfRange(0, size))
                    application.commonAO!!.sendEvent(AoId.AO_CM2_ID, eventBuffer)
                    // send broadcast interrupt
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                }
            }catch (e: IOException) {
                Log.d(tag, "Receive Error: ${e.message}")
                close()
            }
        }
    }

    fun close () {
        Thread {
            try {
                if (connected) {
                    connected = false
                    input.close() // Close input stream
                    output.close() // Close output stream
                    tcpSocket.close() // Close socket
                }
            } catch (e: IOException) {
                Log.d(tag, "Close Error: ${e.message}")
                connected = false
            }
        }.start()
    }
}
