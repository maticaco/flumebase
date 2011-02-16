// (c) Copyright 2011 Odiago, Inc.

package com.odiago.rtengine.exec;

import java.io.IOException;

import java.util.List;
import com.odiago.rtengine.parser.TypedField;

/**
 * Wraps around an EventWrapper for use in a hash table; allows the
 * hash code and equality methods to be defined based on a configurable
 * subset of fields in an event.
 */
class HashedEvent {
  /** The set of fields to use in hashCode() / equals(). */
  private final List<TypedField> mHashFields;

  /** The event itself. */
  private final EventWrapper mEventWrapper;

  public HashedEvent(EventWrapper wrapper, List<TypedField> hashFields) {
    mEventWrapper = wrapper;
    mHashFields = hashFields;
  }

  public EventWrapper getEventWrapper() {
    return mEventWrapper;
  }

  @Override
  public int hashCode() {
    int ret = 0;
    for (TypedField hashField : mHashFields) {
      try {
        Object fieldVal = mEventWrapper.getField(hashField);
        if (null != fieldVal) {
          ret ^= fieldVal.hashCode();
        }
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    boolean first = true;
    for (TypedField hashField : mHashFields) {
      if (!first) {
        sb.append(", ");
      }

      try {
        sb.append(mEventWrapper.getField(hashField));
      } catch (IOException ioe) {
        sb.append("<<IOException: ");
        sb.append(ioe);
        sb.append(">>");
      }
      first = false;
    }

    sb.append(")");
    return sb.toString();
  }

  /** @return true if the set of fields of our EventWrapper we are interested
   * in are equal to the same fields in the other HashedEvent's EventWrapper.
   */
  @Override
  public boolean equals(Object otherObj) {
    if (otherObj == this) {
      return true;
    } else if (null == otherObj) {
      return false;
    } else if (!otherObj.getClass().equals(getClass())) {
      return false;
    }

    HashedEvent other = (HashedEvent) otherObj;

    // We can only be equal if we're comparing the same set of fields.
    if (!mHashFields.equals(other.mHashFields)) {
      return false;
    }

    // Do the field comparison.
    EventWrapper otherEvent = other.mEventWrapper;
    for (TypedField hashField : mHashFields) {
      try {
        Object myVal = mEventWrapper.getField(hashField);
        Object otherVal = otherEvent.getField(hashField);
        if (myVal == null && otherVal != null) {
          return false;
        } else if (myVal != null && otherVal == null) {
          return false;
        } else if (myVal != null && !myVal.equals(otherVal)) {
          return false;
        }
      } catch (IOException ioe) {
        return false;
      }
    }

    return true;
  }
}
