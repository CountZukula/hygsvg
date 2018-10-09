# HygSVG

This is a quick and dirty visualiser for the Hyg database (you can download that at https://github.com/astronexus/HYG-Database, take file `hygdata_v3.csv` and place it in the `input` folder of the project).

## Why was it made?

All the existing tools were either paid or unknown to me, so I took it as a side project.

## What does it do?

You supply the program with a location (latitude and longitude) and a timestamp (in Julian Day notation). Then, it processes the input file and creates a visualisation of the sky, at the given location and time. Output is in SVG format, meaning you can post-process it as much as you want.

## I'm just a software developer

I probably made a lot of mistakes, so if you find any let me know! I based the calculations on the following explanation: http://jknight8.tripod.com/CelestialToAzEl.html#the%20source%20code

## Similar tools
- https://thenightsky.com/
- https://in-the-sky.org/skymap2.php
- ...