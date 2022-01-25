.PHONY: all clean test style coverage jar tree repl run
.SILENT:    clean test style coverage jar tree repl run

.DEFAULT: all

all: clean test

clean:
	if [ -d "target" ]; then rm --recursive target; fi

test:
	clojure -T:build test

style:
	clojure -M:code-formatter fix

coverage:
	clojure -M:code-coverage -p src -s test

jar:
	clojure -T:build ci

tree:
	clojure -Stree

repl:
	clojure -M:repl
