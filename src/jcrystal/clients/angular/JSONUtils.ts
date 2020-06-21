/**
 * Created by AndreaC on 03/04/2017.
 */
export class JSONUtils{
  static jsonQuote(q:string | number | boolean | number[] | number[][]):string{
    if(q instanceof Array){
      if(q.length>0) {
        let q1 = q[0];
        if (q1 instanceof Array) {
          return JSONUtils.jsonQuoteIntMatrix(q as number[][]);
        } else {
          return JSONUtils.jsonQuoteIntArray(q as number[]);
        }
      }
    }else {
      if (typeof(q) === 'string') {
        return JSONUtils.jsonQuoteString(q);
      }
      if (typeof(q) === 'number') {
        return JSONUtils.jsonQuoteNumber(q);
      }
      if (typeof(q) === 'boolean') {
        return JSONUtils.jsonQuoteBool(q);
      }
    }
    return "";

  }
  static jsonQuoteString(text:string):string{
    if(typeof text !== 'undefined' && text!== null && text.length>0){
      let ret = "\"";
      let b:string;
      let c = "0";
      let pos = 0;
      let i : number;
      let len = text.length;
      for (i = 0; i < len; i += 1) {
        b=c;
        c = text.charAt(i);
        switch(c){
          case "\\":
            ret+= "\\\\";
            break;
          case "\"":
            ret += "\\\"";
            break;
          case "/":
            if (b === "<") {
              ret += "\\"
            }
            ret += c;
            break;
          case '\b':
            ret +=  "\\b";
            break;
          case '\t':
            ret +=  "\\t";
            break;
          case '\n':
            ret +=  "\\n";
            break;
          case '\f':
            ret +=  "\\f";
            break;
          case '\r':
            ret +=  "\\r";
            break;
          default:
              ret+= encodeURI(c);
        }

      }
      return ret + "\"";

    }
    return "";
  }

  static jsonQuoteNumber(a: number):string {return String(a);}  //TODO: Decimals
  static jsonQuoteIntArray(a : number[] ):string{
    let ret = "[";
    if(a.length>0)ret += a[0];
    let e = 1;
    for(e=1;e<a.length;e++)
    ret+=","+a[e];
    return ret+"]";
  }
  static jsonQuoteIntMatrix(a : number[][] ):string{
    let ret = "[";
    let pe = false;
    let e = 0;
    for(e = 0; e < a.length; e++, pe = true) {
      if(pe)
        ret+=",[";
      else ret+="[";
      let pi = false;
      let i = 0;
      for (i = 0; i < a[e].length; i++, pi = true) {
        if(pi)
          ret+=",";
        ret+= String(a[e][i]);
      }
      ret+="]";
    }
    return ret+"]";
  }
  static jsonQuoteBool( a: boolean ):string{return a.toString();}
}
