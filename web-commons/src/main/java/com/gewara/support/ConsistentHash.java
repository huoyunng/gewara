package com.gewara.support;

import java.nio.charset.Charset;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class ConsistentHash<T> {
	private SortedMap<Long,T> ketamaNodes=new TreeMap<Long,T>();
    private int numberOfReplicas=1024;
    private HashFunction hashFunction= Hashing.md5(); //guava
    private List<T> nodes;
    private volatile boolean init=false; //标志是否初始化完成

    public ConsistentHash(int numberOfReplicas,List<T> nodes){
        this.numberOfReplicas=numberOfReplicas;
        this.nodes=nodes;

        init();
    }

    public T getNodeByKey(String key){
        if(!init)throw new RuntimeException("init uncomplete...");

        byte[] digest=hashFunction.hashString(key, Charset.forName("UTF-8")).asBytes();
        long hash=hash(digest,0);
        //如果找到这个节点，直接取节点，返回
        if(!ketamaNodes.containsKey(hash)){
            //得到大于当前key的那个子Map，然后从中取出第一个key，就是大于且离它最近的那个key
            SortedMap<Long,T> tailMap=ketamaNodes.tailMap(hash);
            if(tailMap.isEmpty()){
                hash=ketamaNodes.firstKey();
            }else{
                hash=tailMap.firstKey();
            }

        }
        return ketamaNodes.get(hash);
    }

    public synchronized void addNode(T node){
        init=false;
        nodes.add(node);
        init();
    }

    private void init(){
        //对所有节点，生成numberOfReplicas个虚拟节点
        for(T node:nodes){
            //每四个虚拟节点为1组
            for(int i=0;i<numberOfReplicas/4;i++){
                //为这组虚拟结点得到惟一名称
                byte[] digest=hashFunction.hashString(node.toString() +i, Charset.forName("UTF-8")).asBytes();
                //Md5是一个16字节长度的数组，将16字节的数组每四个字节一组，分别对应一个虚拟结点，这就是为什么上面把虚拟结点四个划分一组的原因
                for(int h=0;h<4;h++){
                    Long k = hash(digest,h);
                    ketamaNodes.put(k,node);
                }
            }
        }
        init=true;
    }

    public void printNodes(){
        for(Long key:ketamaNodes.keySet()){
            System.out.println(ketamaNodes.get(key));
        }
    }

    public static long hash(byte[] digest, int nTime)
    {
        long rv = ((long)(digest[3 + nTime * 4] & 0xFF) << 24)
                | ((long)(digest[2 + nTime * 4] & 0xFF) << 16)
                | ((long)(digest[1 + nTime * 4] & 0xFF) << 8)
                | ((long)digest[0 + nTime * 4] & 0xFF);
        return rv;
    }
}
