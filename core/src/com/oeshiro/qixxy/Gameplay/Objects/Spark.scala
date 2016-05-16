package com.oeshiro.qixxy.Gameplay.Objects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.{Vector2, Intersector}
import com.badlogic.gdx.utils.Timer.Task
import com.badlogic.gdx.utils.{Array, Timer}

class Spark(field: GameField, var isClockwise: Boolean)
  extends GameFieldObject(field) {

  val LOG = classOf[Spark].getSimpleName

  sealed abstract class SPARK_STATE
  case object SLEEPING extends SPARK_STATE
  case object MOVING extends SPARK_STATE

  override val size: Float = super.size * 0.8f

  // start at the middle-top of the field
  val startingPosition = new Vector2(
    (field.areaVertices.get(2).x - field.areaVertices.get(3).x) / 2,
    field.areaVertices.get(2).y)

  var state: SPARK_STATE = _
  var currentPath: Array[Vector2] = _
  var timer: Timer = _
  var i_next: Int = _
  var isTrapped: Boolean = _

  val delayTime = 500

  init()

  def init() {
    state = SLEEPING
    timer = new Timer
    position.set(startingPosition)

    currentPath = new Array[Vector2]()
    updatePath()
    i_next = getNextPoint

    isTrapped = false
    // terminal velocity equals to the player's normal one
    terminalVelocity.set(field.player.terminalVelocity)
    velocity.set(terminalVelocity)

    if (timer.isEmpty) {
      // a fuse waits for a moment before continuing
      timer.postTask(new Task {
        override def run() { state = MOVING }
      })
      timer.delay(delayTime)
      timer.start()
    }
  }

  override def render(batch: SpriteBatch, shaper: ShapeRenderer) {
    drawSpark(shaper)
  }

  private def drawSpark(shaper: ShapeRenderer) {
    shaper.setColor(Color.YELLOW)
    shaper.circle(position.x, position.y, size)
    shaper.setColor(Color.WHITE)
  }

  def changeDirection() {
    isClockwise = !isClockwise
  }

  private def updatePath() {
    currentPath.addAll(field.areaVertices)
  }

  private def getNextPoint: Int = {
    var index = 0
    (0 until currentPath.size - 1) foreach { i =>
      if (Intersector.distanceSegmentPoint(currentPath.get(i), currentPath.get(i + 1), position) < 0.01)
        index = if (isClockwise) i else i + 1
    }
    index
  }

  override def update(delta: Float) {
    if (state == MOVING) {
      isTrapped = !isInArea(field.area, position)

      val nextPoint = {
        i_next = if (i_next == currentPath.size) 1
        else if (i_next == -1) currentPath.size - 2
        else i_next
        currentPath.get(i_next)
      }

      // get the next velocity vector
      val vel = nextPoint.cpy()
        .sub(position)
        .nor()
        .scl(velocity.len() * delta)
      val newPos = position.add(vel)
      if (vel.len2() > nextPoint.cpy().sub(position).len2()) {
        newPos.set(nextPoint.cpy())
        i_next += (if (isClockwise) -1 else 1)
      }
      position.set(newPos)
    }
  }
}