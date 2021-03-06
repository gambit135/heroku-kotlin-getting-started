package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jscience.physics.amount.Amount
import org.jscience.physics.model.RelativisticModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.net.URI
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import javax.measure.unit.SI
import javax.sql.DataSource


@Controller
class Controller {

    @Value("\${spring.datasource.url}")
    private var dbUrl: String? = null

    @Autowired
    lateinit private var dataSource: DataSource

    @RequestMapping("/")
    internal fun index(): String {
        return "index"
    }

    @RequestMapping("/db")
    internal fun db(model: MutableMap<String, Any>): String {

        val jdbUri = URI(System.getenv("JAWSDB_URL"))

        val username = jdbUri.userInfo.split(":")[0]
        val password = jdbUri.userInfo.split(":")[1]
        val port:String = jdbUri.port.toString()
        val jdbUrl = "jdbc:mysql://" + jdbUri.host + ":" + port + jdbUri.path

        val connection =  DriverManager.getConnection(jdbUrl, username, password)

        //val connection = dataSource.getConnection()
        try {
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)")
            stmt.executeUpdate("INSERT INTO ticks VALUES (now())")
            val rs = stmt.executeQuery("SELECT tick FROM ticks")

            val output = ArrayList<String>()
            while (rs.next()) {
                output.add("Read from DB: " + rs.getTimestamp("tick"))
            }

            model.put("records", output)
            return "db"
        } catch (e: Exception) {
            connection.close()
            model.put("message", e.message ?: "Unknown error")
            return "error"
        }

    }

    @RequestMapping("/hello")
    internal fun hello(model: MutableMap<String, Any>): String {
        RelativisticModel.select()
        val energy = System.getenv().get("ENERGY");
        val m = Amount.valueOf(energy).to(SI.KILOGRAM)
        model.put("science", "E=mc^2: 12 GeV = $m")
        return "hello"
    }


    @Bean
    @Throws(SQLException::class)
    fun dataSource(): DataSource {
        if (dbUrl?.isEmpty() ?: true) {
            return HikariDataSource()
        } else {
            val config = HikariConfig()
            config.jdbcUrl = dbUrl
            return HikariDataSource(config)
        }
    }
}