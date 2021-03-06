% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/g.R
\name{gset}
\alias{gset}
\title{Assign a value to a paired variable. The value is assigned both locally and remotely.
The local variable must have been initialized by one of the language specific gget.* or
ggset.* functions.}
\usage{
gset(var, value = var)
}
\description{
Assign a value to a paired variable. The value is assigned both locally and remotely.
The local variable must have been initialized by one of the language specific gget.* or
ggset.* functions.
}
\examples{
# Create and initialize a variable in JS
g.js("a = 1")
# Pick up the a variable from JS and define its counterpart in GNUR
gget.js(a)
a
# Increment the variable both locally and in JS 
gset(a, a + 1)
a
g.js("a[0]")
g.js("a[1] = 10")
gget(a)
a
}
\seealso{
Other paired variables: \code{\link{gget.js}},
  \code{\link{gget.rb}}, \code{\link{gget.r}},
  \code{\link{gget}}, \code{\link{gset.js}},
  \code{\link{gset.rb}}, \code{\link{gset.r}}
}
