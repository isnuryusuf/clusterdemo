package org.jboss.demos.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import org.jboss.demos.shared.ClusterInfo;
import org.jboss.demos.shared.ClusterNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:yyang@redhat.com">Yong Yang</a>
 * @create 11/12/12 10:32 AM
 */
public class NodeGroup {

    public static final long lastForStatusChange = 3000;


    private final double width;
    private final double height;
    private final double radius;

    private final int nodeImageWidth = 80;
    private final int nodeImageHeight = 80;

    private final int refreshImageWidth =256;
    private final int refreshImageHeight =256;
    private double refreshAngle = 0;

    private Image nodeImg;
    private boolean nodeImageLoaded;
    private Image refreshImg;
    private boolean refreshImageLoaded;
    private Map<String, Node> nodesMap;

    private double nodeAngle = 0;

    volatile boolean inUpdating = false;
    volatile boolean inDrawing = false;

    private Node currentNode = null;
    private long receivedBytes = 0;
    private long receivedBytesIncrement = 0;
    private boolean isReceiving = false;
    private long receiveStart = 0;



    public NodeGroup(double width, double height, double radius) {
        this.width = width;
        this.height = height;
        this.radius = radius;

        // init logos array
        nodesMap = new HashMap<String, Node>();

        // init image
        nodeImg = new Image("cluster_node-80x80.png");
        nodeImg.addLoadHandler(new LoadHandler() {
            public void onLoad(LoadEvent event) {
                nodeImageLoaded = true;
                // once image is loaded, init logo objects
/*
                ImageElement imageElement = (ImageElement) nodeImg.getElement().cast();
                for (int i = NodeGroup.this.numNodes - 1; i >= 0; i--) {
                    Node node = new Node(null);
                    node.setPosition(NodeGroup.this.width / 2, NodeGroup.this.height / 2);
                    nodesMap.put(node.getIdentity(), node);
                }
*/
            }
        });

        refreshImg = new Image("cluster_receiving-256x256.png");
        refreshImg.addLoadHandler(new LoadHandler() {
            public void onLoad(LoadEvent event) {
                refreshImageLoaded = true;
                // once image is loaded, init logo objects
/*
                ImageElement imageElement = (ImageElement) nodeImg.getElement().cast();
                for (int i = NodeGroup.this.numNodes - 1; i >= 0; i--) {
                    Node node = new Node(null);
                    node.setPosition(NodeGroup.this.width / 2, NodeGroup.this.height / 2);
                    nodesMap.put(node.getIdentity(), node);
                }
*/
            }
        });

        nodeImg.setVisible(false);
        refreshImg.setVisible(false);
        RootPanel.get().add(nodeImg); // image must be on page to fire load
        RootPanel.get().add(refreshImg); // image must be on page to fire load
    }

    public synchronized void updateClusterInfo(ClusterInfo clusterInfo){

        if(inDrawing) {
            return ;
        }

        inUpdating = true;

        List<ClusterNode> clusterNodes = clusterInfo.getClusterNodes();
        System.out.println("UpdateClusterInfo: " + clusterNodes.size() + ", " + Arrays.toString(clusterNodes.toArray()));

        // set receving status
        if(clusterInfo.getReceivedBytes() > receivedBytes){
            receivedBytesIncrement = clusterInfo.getReceivedBytes() - receivedBytes;
            receivedBytes = clusterInfo.getReceivedBytes();
            isReceiving = true;
            receiveStart = System.currentTimeMillis();
        }
        else {
            if(System.currentTimeMillis() - receiveStart > lastForStatusChange) {
                isReceiving = false;
                receivedBytesIncrement = 0;
                receiveStart = 0;
            }
        }


        Map<String, Node> newNodesMap = new HashMap<String, Node>();
        for(ClusterNode clusterNode : clusterNodes) { // look new get clusterNodes
            String id = clusterNode.getIdentity();
            if(nodesMap.containsKey(id)) {
                Node node = nodesMap.remove(id);
                node.updateNodeInfo(clusterNode);
                newNodesMap.put(id, node);
            }
            else {
                Node node = new Node(clusterNode);
                node.setPosition(NodeGroup.this.width / 2, NodeGroup.this.height / 2);
                newNodesMap.put(id, node);
            }
        }
        // the left ones in NodesMap need to be removed
        for(Node node : nodesMap.values()){
            String id = node.getIdentity();
            node.setRemoving();
            if(!node.isRemoved()) { // if is time to to remove, remove it, else re-put
                newNodesMap.put(id, node);
            }
        }
        nodesMap.clear();
        nodesMap.putAll(newNodesMap);
        inUpdating = false;
    }

    synchronized void draw(Context2d context, int mouseX, int mouseY) {
        if (!nodeImageLoaded) {
            return;
        }
        if (!refreshImageLoaded) {
            return;
        }

        if(inUpdating) {
            return;
        }

        this.currentNode = null;
        inDrawing = true;
        nodeAngle = (nodeAngle + Math.PI/2.0 * 0.003);


        List<Node> nodes = new ArrayList<Node>(nodesMap.values());


        Collections.sort(nodes, new Comparator<Node>() {
            public int compare(Node o1, Node o2) {
                return o1.getIdentity().compareTo(o2.getIdentity());
            }
        });
        int numNodes = nodes.size();

        //TODO: re-calculate position when remove and new
        for (int i = numNodes - 1; i >= 0; i--) {
            Node node = nodes.get(i);

            // update position
            double perPI = 2 * Math.PI * i / numNodes;
            Vector goal = new Vector(width / 2 + radius * Math.cos(nodeAngle + perPI),
                    height / 2 + radius * Math.sin(nodeAngle + perPI));
            node.setPosition(goal.getX(), goal.getY());
        }

/*
        context.beginPath();
        context.setFillStyle(CssColor.make("blue"));
        context.arc(100, 100, 100, 0, Math.PI);
        context.fill();
        context.closePath();
*/
        for (Node node  : nodesMap.values()) {

            context.save();
            context.beginPath();

            //onMouseOver, shadow
            if(mouseX > 0 && mouseY > 0 &&  mouseX - node.getPosition().getX() > 0 &&  mouseX - node.getPosition().getX() < nodeImageWidth  && mouseY - node.getPosition().getY() > 0 && mouseY - node.getPosition().getY() < nodeImageHeight ) {
//                System.out.println("shadow, node: " + node.getPosition().getX() + ", " + node.getPosition().getY() + "; mouse: " + mouseX + ", " + mouseY);
                this.currentNode = node;
                context.setShadowOffsetX(5);
                context.setShadowOffsetY(5);
                context.setShadowBlur(30);
                context.setShadowColor("black");
            }

            //Removing
            if(node.isRemoving()) {
                context.setShadowOffsetX(5);
                context.setShadowOffsetY(5);
                context.setShadowBlur(50);
                context.setShadowColor("red");
//                context.setGlobalAlpha(0.8);
            }

            //Starting
            if(node.isStarting()) {
                context.setShadowOffsetX(5);
                context.setShadowOffsetY(5);
                context.setShadowBlur(50);
                context.setShadowColor("green");

            }

            //Reloading
            if(node.isReloading()) {
                context.setShadowOffsetX(5);
                context.setShadowOffsetY(5);
                context.setShadowBlur(50);
                context.setShadowColor("yellow");
            }


            context.translate(node.getPosition().getX(), node.getPosition().getY());
            context.drawImage((ImageElement) nodeImg.getElement().cast(), 0, 0);

            // memory usage bar
            double memUsage = node.getClusterNode().getMemUsage();
            if(memUsage < 0.3) {
                context.setFillStyle(CssColor.make("green"));
            }
            else if(memUsage < 0.6) {
                context.setFillStyle(CssColor.make("orange"));
            }
            else {
                context.setFillStyle(CssColor.make("red"));
            }
            context.fillRect(0, nodeImageHeight*(1-memUsage), 5, nodeImageHeight*memUsage);

            // thread usage bar
            double threadUsage = node.getClusterNode().getThreadUsage();
            if(threadUsage < 0.3) {
                context.setFillStyle(CssColor.make("green"));
            }
            else if(threadUsage < 0.6) {
                context.setFillStyle(CssColor.make("orange"));
            }
            else {
                context.setFillStyle(CssColor.make("red"));
            }
            context.fillRect(nodeImageWidth-5, nodeImageHeight*(1-threadUsage), 5, nodeImageHeight*threadUsage);
            // ip
            context.setFillStyle(CssColor.make("blue"));
            if(node.equals(currentNode)) {
                // show percentage on current node
                NumberFormat numberFormat = NumberFormat.getPercentFormat();
                context.fillText(numberFormat.format(memUsage),  1, 8);
                context.fillText(numberFormat.format(threadUsage),  nodeImageWidth-20, 8);
            }
            context.fillText(node.getIdentity(), 0, nodeImageHeight+20);
            context.closePath();
            context.restore();
        }
        if(isReceiving) { // channel receiving
//          context.setFillStyle(CssColor.make("blue"));
//          context.rect(0, 0, nodeImageWidth, nodeImageHeight);
//          context.fillRect(0, 0, 5, nodeImageHeight);
            context.save();
//            context.setGlobalAlpha(0.5);
            context.translate(ClusterDemo.width/2, ClusterDemo.height/2);
            refreshAngle += Math.PI/90;
            context.rotate(refreshAngle);
            context.drawImage((ImageElement) refreshImg.getElement().cast(), -refreshImageWidth / 2, -refreshImageHeight / 2);
            context.restore();
            context.save();
            context.setFont("40pt sans-serif");
            context.setFillStyle("gray");
            context.setTextBaseline(Context2d.TextBaseline.MIDDLE);
            String incrementText = "+" + receivedBytesIncrement + "B";
            double textWidth = context.measureText(incrementText).getWidth();
            context.fillText(incrementText, (ClusterDemo.width-textWidth)/2, ClusterDemo.height/2);
            context.restore();
        }
        inDrawing = false;
    }


    public Node getCurrentNode() {
        return currentNode;
    }

    private ImageData scaleImage(Image image, double scaleToRatio) {
        Canvas canvasTmp = Canvas.createIfSupported();
        Context2d context = canvasTmp.getContext2d();

        double ch = (image.getHeight() * scaleToRatio) + 100;
        double cw = (image.getWidth() * scaleToRatio) + 100;

        canvasTmp.setCoordinateSpaceHeight((int) ch);
        canvasTmp.setCoordinateSpaceWidth((int) cw);

        ImageElement imageElement = ImageElement.as(image.getElement());

        // s = source
        // d = destination
        double sx = 0;
        double sy = 0;
        double sw = imageElement.getWidth();
        double sh = imageElement.getHeight();

        double dx = 0;
        double dy = 0;
        double dw = imageElement.getWidth();
        double dh = imageElement.getHeight();

        // tell it to scale image
        context.scale(scaleToRatio, scaleToRatio);

        // draw image to canvas
        context.drawImage(imageElement, sx, sy, sw, sh, dx, dy, dw, dh);

        // get image data
        double w = dw * scaleToRatio;
        double h = dh * scaleToRatio;
        ImageData imageData = context.getImageData(0, 0, w, h);
        return imageData;
    }

}
