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

### Indexer

```bash
Indexer <index>|"-" <file or directory>*|"-"
```

- **\<index\>** is the directory where the resulting index files will be stored.
This directory will be created if it doesn't exist yet, and cleared if it does
exist. This argument can also be replaced by a literal `-`, in which case a
default directory (`./Index`) is used.

- **\<file or directory\>** is a file or directory that should be indexed.
Directories will be traversed recursively. This means that all files in the
directory, or any of its subdirectories, will be added to the index. Hidden
and unreadable files will be skipped. Hidden files can, however, still be
indexed if the user explicitly adds them to the argument list.
If a single **\<file or directory\>** is given that is `-`, then a default
file (`./Posts.xml`) is indexed.

The output of executing this file looks like this (5206682ms &#8776; 86.8min):

```txt
Creating index in directory ./Index
50337841 documents indexed, time: 5206682ms
```

### Searcher

```bash
Searcher <index>|"-" <SO dump>|"-"|"!" <query parameter>*
```

- **\<index\>** is the directory where the index, in which to search, is
located. The default (`./Index`) can also be used by substituting this
parameter with a literal `-`.

- **\<SO dump\>** is the SO dump file from which the index was created. The
default (`./Posts.xml`) can, again, be substituted with `-`. If the index
wasn't created from the SO dump, then a literal `!` should be given.

- **\<query parameter\>** is a single query parameter. An overview of the syntax
can be found on [this page](
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

