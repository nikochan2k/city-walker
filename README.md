# City Walker

## Overview

City Walker is CityGML converter to various formats.
But now, City Walker can only convert to GeoJSON with LOD1.

## Download

Please download from Releases page
https://github.com/nikochan2k/city-walker/releases

## Usage

java city-walker ...

    Usage: city-walker [-fhnV] [-d=<outputSRS>] [-o=<outputDir>] [-s=<inputSRS>]
    -t=<type> [FILE...]
    Convert CityGML to various formats
    [FILE...] Glob pattern of file path.
    -d, --dst=<outputSRS> Destination SRS (Default: EPSG:4326)
    -f, --flipXY flip X and Y coordinate
    -h, --help Show this help message and exit.
    -n, --no-attr No attribute except for measuredHeight
    -o, --output=<outputDir> Output directory (Default: the same directory with
    input file)
    -s, --src=<inputSRS> Source SRS (Default: Try to detect, or EPSG:4326)
    -t, --type=<type> Output format type
    -V, --version Print version information and exit.
