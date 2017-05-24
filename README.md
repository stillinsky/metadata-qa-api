# Metadata Quality Assurance Framework API

This project is the central piece of the Metadata Quality Assurance Framework, every other project is 
built on top of it. It provides a general framework for measuring metadata quality in different 
digital collections.

If you want to implement it to your collection you have to define a schema, which is a kind of
presentation of an existing metadata schema, and configure the basic facade, which will run the calculation.

```Java
CalculatorFacade calculator = new CalculatorFacade();
// do some configuration with the accessor of calculator Facade
for (String jsonRecord : jsonRecords) {
    try {
        String csv = calculator.measure(jsonRecord);
        // save csv
    } catch (InvalidJsonException e) {
        // handle exception
    }
}

```

For the usage and implementation of the API see https://github.com/pkiraly/europeana-qa-api.

[![Build Status](https://travis-ci.org/pkiraly/metadata-qa-api?branch=master)](https://travis-ci.org/pkiraly/metadata-qa-api) [![Coverage Status](https://coveralls.io/repos/github/pkiraly/metadata-qa-api/badge.svg?branch=master)](https://coveralls.io/github/pkiraly/metadata-qa-api?branch=master)
