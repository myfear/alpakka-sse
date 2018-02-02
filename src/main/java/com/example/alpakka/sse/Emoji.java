package com.example.alpakka.sse;

import akka.http.javadsl.model.sse.ServerSentEvent;

/**
 *
 * @author myfear
 */
public class Emoji {
  public final Integer count;
  public final String name;

  
  public Emoji(Integer _id, String _name) {
    count = _id;
    name = _name;
  }

    @Override
    public String toString() {
        return "User{" + "id=" + count + ", name=" + name + '}';
    }
  
  
  

  @Override
  public int hashCode() {
      int hash = 3;
      hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
      hash = 53 * hash + this.count;
      return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if (!Emoji.class.isAssignableFrom(obj.getClass())) {
      return false;
    }
    final Emoji other = (Emoji) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
        return false;
    }
    if (this.count != other.count) {
        return false;
    }
    return true;
  
  }
  

  
  
}