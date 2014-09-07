build:
	lein do cljsbuild clean, cljsbuild once release

dev:
	sass --watch resources/dev/scss/main.scss:resources/public/css/style.css &  lein figwheel

css:
	sass resources/dev/scss/main.scss:resources/public/css/style.css
