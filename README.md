# ElectrumVisualizer - OUTDATED STUDENT PROJECT

This is a fork of the [Electrum](https://github.com/nmacedo/Electrum) project, which is itself an extension of the Alloy Analyzer project.

## Electrum

*(taken from https://github.com/nmacedo/Electrum)*

This extension to the Alloy Analyzer by the [High-Assurance Software Laboratory](http://haslab.di.uminho.pt) provides an analyzer for Electrum models, a temporal extension to the Alloy modeling language.

[Alloy](http://alloy.mit.edu/) is a simple structural modeling language based on first-order logic developed at the [Software Design Group](http://sdg.csail.mit.edu/). Its Analyzer can generate instances of invariants, simulate the execution of operations, and check user-specified properties of a model.

Electrum is open-source and available under the [MIT license](LICENSE), as is the Alloy Analyzer. However, it utilizes several third-party packages whose code may be distributed under a different license (see the various LICENSE files in the distribution for details), including [Kodod](https://github.com/emina/kodkod)'s extension [Pardinus](https://github.com/nmacedo/Pardinus) and its underlying solvers ([SAT4J](http://www.sat4j.org), [MiniSat](http://minisat.se), [Glucose/Syrup](http://www.labri.fr/perso/lsimon/glucose/), [(P)Lingeling](http://fmv.jku.at/lingeling/), and [Yices](http://yices.csl.sri.com)), as well as [CUP](http://www2.cs.tum.edu/projects/cup/) and [JFlex](http://jflex.de/) to generate the parser.

## Building ElectrumVisualizer

You need `perl` to build the project.

To build ElectrumVisualizer :
```
perl build build
```

You can clean your project (removing all the .class) with :
```
perl build clean
```

Then you can run Alloy Analyzer with :
```
perl build run
```

You can also build a runnable .jar with :
```
perl build jar
```

Some help on the script is available with :
```
perl build help
```

## Collaborators
- Nuno Macedo, HASLab, Portugal
- Julien Brunel, ONERA, France
- David Chemouil, ONERA, France
- Alcino Cunha, HASLab, Portugal
- Denis Kuperberg TU Munich, Germany
- Eduardo Pessoa, HASLab, Portugal
- Rémi Bossut, ENSEEIHT, France
- Guillaume Dupont, ENSEEIHT, France
- Louis Fauvarque, ENSEEIHT, France
- Maxime Quentin, ENSEEIHT, France
- Rémi Bossut, ENSEEIHT, France

## History

### ElectrumAnalyzer 0.0.1

First initial commit, forked from Electrum.
