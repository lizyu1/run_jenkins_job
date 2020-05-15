#!/usr/bin/groovy

try {
  def vcc_list = []
  def rawData = 'curl -s https://ext.somesite/vcc'.execute().text
  rawData = rawData.replaceAll("\\<.*?\\>","");
  def findData = '"data":[['
  def lines = rawData.readLines()
  def hasData = { it.contains(findData)}
  if (lines.any(hasData)){
    rawData1 = lines.find(hasData)
    rawData1 = rawData1.replaceFirst(".*(?=\\[\\[)","").replaceFirst("(?=\\]\\]).*","\\]\\]")
    rawData2 = rawData1.substring(1,rawData1.length()-1).replaceAll(",\\[","\\[","\\[\\").split(',\\[')
    for (vcc in rawData2){
     vcc = vcc.replaceAll("\"","").tokenize(',[]')
     vcc_list.add(vcc[0])
    }
    return vcc_list.sort()
  } else {
    println "data not found"
    system.exit(0)
  }
} catch (Exception e){
    println(e)
}

