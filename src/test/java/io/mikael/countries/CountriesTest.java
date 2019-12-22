package io.mikael.countries;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

public class CountriesTest {

    private static Thread MAIN;

    @AfterClass
    public static void tearDown() throws Exception {
        ReactorCountries.DISPOSABLE.disposeNow();
        MAIN.interrupt();
    }

    @BeforeClass
    public static void setup() throws Exception {
        RestAssured.defaultParser = Parser.JSON;
        MAIN = new Thread(ReactorCountries::main);
        MAIN.start();
        while (null == ReactorCountries.START) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    @Before
    public void setUp() {
        RestAssured.port = 8080;
    }

    @Test
    public void finland() {
        when().
                get("/countries/{cca2}", "FI").
        then().
                statusCode(HttpStatus.SC_OK).
                body("name.common", is("Finland")).
                body("region", is("Europe"));
    }

    @Test
    public void sweden() {
        when().
                get("/countries/{cca2}", "SE").
        then().
                statusCode(HttpStatus.SC_OK).
                body("name.common", is("Sweden")).
                body("region", is("Europe"));
    }

}
