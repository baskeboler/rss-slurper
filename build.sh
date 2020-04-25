#!/usr/bin/env bash 

rm -rf resources/public/js/cljs-runtime
clj -A:shadow-cljs release app 
clj -A:uberjar 
