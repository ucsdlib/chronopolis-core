%define __jar_repack {%nil}
# Compatibility with ncar
%define _source_payload w0.gzdio
%define _binary_payload w0.gzdio
%define _binary_filedigest_algorithm 1

# For use below
%define _prefix %{_usr}/local/chronopolis/replication
%define jar replicationd.jar
%define yaml application.yml
%define initsh /etc/init.d/replicationd
%define build_date %(date +"%Y%m%d")

Name: replicationd
Version: %{ver}
Release: %{build_date}.el6
Source: replication-shell.jar
Source1: replication.sh
Source2: application.yml
Summary: Chronopolis Replication Service
License: UMD
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Replication Services monitors for packages being ingested into Chronopolis
and does replication and registration on them.

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"
%__install -D -m0755 "%{SOURCE1}" "%{buildroot}%{initsh}"

%__install -d "%{buildroot}/var/log/chronopolis"

%files

%defattr(-,root,root)
%dir %{_prefix}
%{_prefix}/%{jar}
%config(noreplace) %{_prefix}/%{yaml}
%{initsh}

%dir %attr(0755,-,-) /var/log/chronopolis

%post

chkconfig --add  replication

%preun

chkconfig --del replication

%changelog

* Tue Oct 3 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171003
- cleanup spec to include missing sections
