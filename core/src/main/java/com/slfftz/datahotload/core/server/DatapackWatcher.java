@@
     private void scheduleDebouncedReload(String name) {
@@
     }
+
+    /**
+     * Whether the watcher is currently running.
+     */
+    public boolean isRunning() {
+        return running.get();
+    }
+
+    /**
+     * Return the last changed filename observed by the watcher.
+     */
+    public String getLastChangedName() {
+        return lastChangedName;
+    }
@@
     @Override
     public void close() {
@@
     }
 }
