# Information Retrieval: Assignment 1

Darin Verheijke (s0154975)
Jules Desmet (s0162381)

## Usage

The code for this assignment can be found in the `src/` directory. The two main
files are `Indexer.java` and `Searcher.java`, which create an index and search
in an index, respectively.

Both files can be executed with the appropriate arguments, which we will explain
below. The base command to run the code is this (assuming the code is being
executed from the project root):

```bash
java -classpath out/production/Lucene:lib/* com.informationretrieval.lucene.
```

This should then be concatenated with one of the following commands.

In short, the following commands can be used to index the stackoverflow dump,
and to search through the index. (In this case we assume that you're in the
project root, that the index will be stored in `./Index`, and that the XML dump
can be found in `./Posts.xml`.)

```bash
Indexer -d
Searcher <query>
```

### Indexer

```bash
Indexer [-i|--index <index>] [-d|--dump] <document>*
```

- **\<index\>** is the directory where the resulting index files will be stored.
This directory will be created if it doesn't exist yet, and cleared if it does
exist. If this is not specified, then the default `./Index` is used.

- **\<document\>** is a file that should be indexed. This could also be a
directory, in which case all of its files are indexed, and (recursively) all of
its subdirectories' files. If `-d|--dump` is also included in the argument
string, then this list is allowed to be empty, and the default `./Posts.xml` is
indexed.

The output of executing this file looks like this (5206682ms &#8776; 86.8min):

```txt
Creating index in directory ./Index
50337841 documents indexed, time: 5206682ms
```

### Searcher

```bash
Searcher [-i|--index <index>] [-d|--dump <SO dump>] <query parameter>+
```

- **\<index\>** is the directory where the index in which to search, is located.
If this option is not specified, then the default `./Index` is used.

- **\<SO dump\>** is the SO dump file from which the index was created. If this
option is not specified, then the default `./Posts.xml` is used. If the index
was not created from the SO dump, then this option can be ignored.

- **\<query parameter\>+** is the list of query parameters. There should always
be at least 1 parameter. An overview of the syntax can be found on [this page](
https://lucene.apache.org/core/8_6_3/queryparser/index.html#package.description
).

## Implementation

Only a couple of attributes of each post is used in the index. The main
attribute is, of course, the `Body` field. For questions (indicated by
`PostTypeId="1"`) we also index the title of the post. For answers (indicated by
`PostTypeId="2"`), on the other hand, we also include the parent's ID. This
parent is the associated question. And last but not least, the post's ID is also
indexed.

The index only stores the posts' ID. This reduces the final size of the index,
as well as the time it takes to construct the index. Not storing the additional
fields (`Body`, `Title`, and `ParentId`) reduced the size of the index from
`46G` to `17G` (computed using `du -hs Index`). The decrease in time was over
40 minutes.

This also means that the searcher can only return the post's ID. But, by using
the original XML file, we can still get the full information for each post. To
do this we implemented a binary search algorithm for the XML file, because the
posts in that file are ordered on their `Id` attribute. (This algorithm can be
found in [Searcher class](./src/com/informationretrieval/lucene/Searcher.java)
in the `getPost()` function.) By using this algorithm we can, after getting the
results from Lucene, go over the query results and get each post's full data.

