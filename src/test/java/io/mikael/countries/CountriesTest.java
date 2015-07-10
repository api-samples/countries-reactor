package io.mikael.countries;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

public class CountriesTest {

    @AfterClass
    public static void tearDown() throws Exception {
        ReactorCountries.SERVER.shutdown().awaitSuccess();
    }

    @BeforeClass
    public static void setup() throws Exception {
        RestAssured.defaultParser = Parser.JSON;
        ReactorCountries.main();
    }

    @Before
    public void setUp() {
        RestAssured.port = ReactorCountries.SERVER.getListenAddress().getPort();
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
