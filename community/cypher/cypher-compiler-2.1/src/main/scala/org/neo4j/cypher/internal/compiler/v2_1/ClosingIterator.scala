/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_1

import org.neo4j.cypher.internal.helpers._
import org.neo4j.cypher.NodeStillHasRelationshipsException
import org.neo4j.graphdb.ConstraintViolationException
import org.neo4j.graphdb.TransactionFailureException
import scala.collection
import scala.collection.mutable
import org.neo4j.cypher.internal.compiler.v2_1.spi.QueryContext

trait CleanupTaskList {
  def cleanupTasks: Seq[() => Unit]
}

class ClosingIterator(inner: Iterator[collection.Map[String, Any]], queryContext: QueryContext, cleanupTaskList: CleanupTaskList) extends Iterator[Map[String, Any]] {
  private var closed: Boolean = false
  lazy val still_has_relationships = "Node record Node\\[(\\d),.*] still has relationships".r

  def hasNext: Boolean = failIfThrows {
    val innerHasNext: Boolean = inner.hasNext
    if (!innerHasNext) {
      close(true)
    }
    innerHasNext
  }

  def next(): Map[String, Any] = failIfThrows {
    val input: collection.Map[String, Any] = inner.next()
    val result: Map[String, Any] = Materialized.mapValues(input, materialize)
    if (!inner.hasNext) {
      close(true)
    }
    result
  }

  private def materialize(v: Any): Any = v match {
    case (x: Stream[_])   => x.map(materialize).toList
    case (x: Map[_, _])   => Materialized.mapValues(x, materialize)
    case (x: Iterable[_]) => x.map(materialize)
    case x                => x
  }

  def close() {
    close(true)
  }

  def close(success: Boolean) = translateException {
    if (!closed) {
      closed = true
      val first = () =>
        queryContext.close(success)
      val taskFunctions = {
        val builder: mutable.Builder[() => Unit, Seq[() => Unit]] = Seq.newBuilder
        builder += first
        builder ++= cleanupTaskList.cleanupTasks.map { task =>
          () => task.apply()
        }
        builder.result
      }
      taskFunctions.flatMap { f =>
        try {
          f()
          None
        } catch {
          case e: Throwable => Some(e)
        }
      }.headOption match {
        case Some(e) => throw e
        case None =>
      }
    }
  }

  private def translateException[U](f: => U): U = {
    try {
      f
    } catch {
      case e: TransactionFailureException =>
        var cause: Throwable = e
        while (cause.getCause != null) {
          cause = cause.getCause
          if (cause.isInstanceOf[ConstraintViolationException]) {
            cause.getMessage match {
              case still_has_relationships(id) => throw new NodeStillHasRelationshipsException(id.toLong, e)
              case _                           => throw e
            }
          }
        }

        throw e
    }
  }

  private def failIfThrows[U](f: => U): U = try {
    f
  } catch {
    case t: Throwable =>
      close(false)
      throw t
  }
}
